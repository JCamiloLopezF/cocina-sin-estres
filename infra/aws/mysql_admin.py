"""KAN-9 · Tareas de administrador sobre el MySQL de Amazon RDS.

Lo usan crear-base-datos.sh y verificar-base-datos.sh desde AWS CloudShell, que no
trae cliente de MySQL. Usa PyMySQL (Python puro) y siempre se conecta con TLS,
verificando el certificado de RDS.

Variables de entorno (nunca argumentos, para que no queden en la lista de procesos):
  DB_HOST, DB_ADMIN_USUARIO, DB_ADMIN_CONTRASENA, DB_CA (bundle de certificados de RDS)
  Para "crear": CONTRASENA_DESARROLLO y CONTRASENA_PRODUCCION

Uso:
  python3 mysql_admin.py crear     # bases y usuarios por ambiente
  python3 mysql_admin.py revisar   # permisos y migraciones de Flyway
"""

import os
import sys

import pymysql

AMBIENTES = ("desarrollo", "produccion")


def conectar():
    return pymysql.connect(
        host=os.environ["DB_HOST"],
        port=int(os.environ.get("DB_PUERTO", "3306")),
        user=os.environ["DB_ADMIN_USUARIO"],
        password=os.environ["DB_ADMIN_CONTRASENA"],
        ssl={"ca": os.environ["DB_CA"]},
        connect_timeout=15,
        autocommit=True,
        charset="utf8mb4",
    )


def crear(cursor):
    for ambiente in AMBIENTES:
        base = f"cocina_{ambiente}"
        usuario = f"app_{ambiente}"
        contrasena = os.environ[f"CONTRASENA_{ambiente.upper()}"]
        cursor.execute(
            f"CREATE DATABASE IF NOT EXISTS `{base}` "
            "CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
        )
        # Con parámetros, PyMySQL usa formato %, por eso el host comodín va como '%%'.
        # REQUIRE SSL: además del parámetro require_secure_transport del servidor.
        cursor.execute(
            f"CREATE USER IF NOT EXISTS '{usuario}'@'%%' IDENTIFIED BY %s REQUIRE SSL",
            (contrasena,),
        )
        cursor.execute(
            f"ALTER USER '{usuario}'@'%%' IDENTIFIED BY %s REQUIRE SSL", (contrasena,)
        )
        # Todos los permisos, pero solo sobre su base: Flyway crea y altera las tablas (ADR-07).
        cursor.execute(f"GRANT ALL PRIVILEGES ON `{base}`.* TO '{usuario}'@'%'")
        print(f"  {base} y {usuario}: listos")


def revisar(cursor):
    for ambiente in AMBIENTES:
        base = f"cocina_{ambiente}"
        usuario = f"app_{ambiente}"
        print(f"\n== {usuario} ==")
        cursor.execute(f"SHOW GRANTS FOR '{usuario}'@'%'")
        for (permiso,) in cursor.fetchall():
            print(f"  {permiso}")
        cursor.execute(
            "SELECT ssl_type FROM mysql.user WHERE user = %s AND host = '%%'", (usuario,)
        )
        fila = cursor.fetchone()
        print(f"  Exige TLS: {'sí' if fila and fila[0] else 'no'}")

        print(f"\n== Migraciones en {base} ==")
        cursor.execute(
            "SELECT COUNT(*) FROM information_schema.tables "
            "WHERE table_schema = %s AND table_name = 'flyway_schema_history'",
            (base,),
        )
        if cursor.fetchone()[0] == 0:
            print("  Todavía sin migraciones: Flyway las aplica la primera vez que la API arranca contra esta base.")
            continue
        cursor.execute(
            f"SELECT version, description, installed_on, success "
            f"FROM `{base}`.flyway_schema_history ORDER BY installed_rank"
        )
        for version, descripcion, aplicada, exito in cursor.fetchall():
            estado = "ok" if exito else "FALLÓ"
            print(f"  V{version}  {descripcion}  ({aplicada:%Y-%m-%d %H:%M})  {estado}")


def main():
    if len(sys.argv) != 2 or sys.argv[1] not in ("crear", "revisar"):
        print(__doc__)
        return 2
    try:
        conexion = conectar()
    except pymysql.err.OperationalError as error:
        print(f"ERROR: no pude conectarme a MySQL: {error}", file=sys.stderr)
        return 1
    with conexion, conexion.cursor() as cursor:
        crear(cursor) if sys.argv[1] == "crear" else revisar(cursor)
    return 0


if __name__ == "__main__":
    sys.exit(main())
