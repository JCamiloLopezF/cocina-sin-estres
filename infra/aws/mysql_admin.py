"""KAN-9 · Tareas de administrador sobre el MySQL de Amazon RDS.

Lo usan crear-base-datos.sh y verificar-base-datos.sh desde AWS CloudShell, que no
trae cliente de MySQL. Usa PyMySQL (Python puro) y siempre se conecta con TLS,
verificando el certificado de RDS.

Variables de entorno (nunca argumentos, para que no queden en la lista de procesos):
  DB_HOST, DB_ADMIN_USUARIO, DB_ADMIN_CONTRASENA, DB_CA (bundle de certificados de RDS)
  Para "crear": CONTRASENA_DESARROLLO y CONTRASENA_PRODUCCION
  Para "limitar": CONTRASENA_MIGRACION_DESARROLLO y CONTRASENA_MIGRACION_PRODUCCION

Uso:
  python3 mysql_admin.py crear     # bases y usuarios por ambiente
  python3 mysql_admin.py revisar   # permisos y migraciones de Flyway
  python3 mysql_admin.py limitar   # usuario de migraciones aparte; la API solo con datos
  python3 mysql_admin.py contar    # filas por tabla y última migración (prueba de restauración)
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


def limitar(cursor):
    """Permisos mínimos (auditoría del 4 de noviembre).

    Flyway cambia el esquema con migra_<ambiente>, que tiene todos los permisos sobre su
    base. La API usa app_<ambiente>, que solo lee y escribe datos: si alguien lograra
    ejecutar SQL a través de ella, no podría borrar tablas ni cambiar el esquema.
    """
    for ambiente in AMBIENTES:
        base = f"cocina_{ambiente}"
        app = f"app_{ambiente}"
        migra = f"migra_{ambiente}"
        contrasena = os.environ[f"CONTRASENA_MIGRACION_{ambiente.upper()}"]
        cursor.execute(
            f"CREATE USER IF NOT EXISTS '{migra}'@'%%' IDENTIFIED BY %s REQUIRE SSL", (contrasena,)
        )
        cursor.execute(f"ALTER USER '{migra}'@'%%' IDENTIFIED BY %s REQUIRE SSL", (contrasena,))
        cursor.execute(f"GRANT ALL PRIVILEGES ON `{base}`.* TO '{migra}'@'%'")
        cursor.execute(f"REVOKE ALL PRIVILEGES ON `{base}`.* FROM '{app}'@'%'")
        cursor.execute(f"GRANT SELECT, INSERT, UPDATE, DELETE ON `{base}`.* TO '{app}'@'%'")
        print(f"  {migra}: todos los permisos sobre {base} (solo para Flyway)")
        print(f"  {app}: SELECT, INSERT, UPDATE y DELETE sobre {base}")


TABLAS_CLAVE = (
    "usuario", "hogar", "persona", "producto", "receta", "ingrediente_receta",
    "item_despensa", "plan_semanal", "comida_plan", "lista_compras", "aviso",
)


def contar(cursor):
    """Evidencia de la prueba de restauración (RNF-20): la base restaurada tiene datos."""
    base = "cocina_produccion"
    cursor.execute(
        f"SELECT version, description, installed_on FROM `{base}`.flyway_schema_history "
        "WHERE success = TRUE ORDER BY installed_rank DESC LIMIT 1"
    )
    version, descripcion, aplicada = cursor.fetchone()
    print(f"\n== {base}: última migración V{version} {descripcion} ({aplicada:%Y-%m-%d %H:%M}) ==")
    cursor.execute(
        "SELECT table_name FROM information_schema.tables WHERE table_schema = %s", (base,)
    )
    existentes = {fila[0] for fila in cursor.fetchall()}
    print(f"  Tablas: {len(existentes)}")
    for tabla in TABLAS_CLAVE:
        if tabla not in existentes:
            print(f"  {tabla:20} NO EXISTE")
            continue
        cursor.execute(f"SELECT COUNT(*) FROM `{base}`.`{tabla}`")
        print(f"  {tabla:20} {cursor.fetchone()[0]:>8} filas")


def revisar(cursor):
    for ambiente in AMBIENTES:
        base = f"cocina_{ambiente}"
        usuario = f"app_{ambiente}"
        print(f"\n== {usuario} ==")
        cursor.execute(f"SHOW GRANTS FOR '{usuario}'@'%'")
        for (permiso,) in cursor.fetchall():
            print(f"  {permiso}")
        cursor.execute("SELECT COUNT(*) FROM mysql.user WHERE user = %s", (f"migra_{ambiente}",))
        if cursor.fetchone()[0]:
            print(f"  Migraciones con migra_{ambiente} (permisos mínimos activos)")
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
    acciones = {"crear": crear, "revisar": revisar, "limitar": limitar, "contar": contar}
    if len(sys.argv) != 2 or sys.argv[1] not in acciones:
        print(__doc__)
        return 2
    try:
        conexion = conectar()
    except pymysql.err.OperationalError as error:
        print(f"ERROR: no pude conectarme a MySQL: {error}", file=sys.stderr)
        return 1
    with conexion, conexion.cursor() as cursor:
        acciones[sys.argv[1]](cursor)
    return 0


if __name__ == "__main__":
    sys.exit(main())
