#!/usr/bin/env bash
# Prueba la migración V1 contra un MySQL real: casos válidos, reglas que deben
# rechazar datos y borrados en cascada. Usa una base temporal (cocina_prueba)
# que crea y borra, así que no toca tus datos de desarrollo.
#
# Uso, con el MySQL del docker-compose encendido:
#   bash scripts/probar-esquema.sh
# Variables opcionales: DB_ROOT_CONTRASENA (por defecto root_local), MYSQL_CMD.
set -u
cd "$(dirname "$0")/.."
export MYSQL_PWD="${DB_ROOT_CONTRASENA:-root_local}"
if [[ -n "${MYSQL_CMD:-}" ]]; then
  MYSQL="$MYSQL_CMD"
elif command -v mysql >/dev/null 2>&1; then
  MYSQL="mysql -h 127.0.0.1 -P 3306 -uroot"
else
  # Sin cliente mysql en el computador: se usa el que trae el contenedor del docker-compose
  MYSQL="docker compose exec -T -e MYSQL_PWD mysql mysql -uroot"
fi
DB=cocina_prueba
MIGRACION=backend/src/main/resources/db/migration/V1__esquema_inicial.sql

$MYSQL -e "DROP DATABASE IF EXISTS $DB; CREATE DATABASE $DB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" || { echo "No pude conectarme a MySQL. ¿Está encendido? docker compose up -d mysql"; exit 1; }
$MYSQL "$DB" < "$MIGRACION" || { echo "La migración falló al aplicarse"; exit 1; }
echo "Migración aplicada en la base temporal $DB"

q()  { $MYSQL -N -B "$DB" -e "$1" 2>&1; }
ok() { out=$(q "$2"); if [[ $? -eq 0 && "$out" != *ERROR* ]]; then echo "PASA   $1"; else echo "FALLA  $1 -> $out"; fails=$((fails+1)); fi; }
ko() { out=$(q "$2"); if [[ "$out" == *ERROR* ]]; then echo "PASA   $1 (rechazado)"; else echo "FALLA  $1 -> se aceptó y debía rechazarse"; fails=$((fails+1)); fi; }
eq() { out=$(q "$2"); if [[ "$out" == "$3" ]]; then echo "PASA   $1"; else echo "FALLA  $1 -> obtuvo '$out', esperaba '$3'"; fails=$((fails+1)); fi; }
fails=0

echo "== Estructura"
eq "17 tablas creadas" "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$DB'" "17"
eq "todas InnoDB + utf8mb4_unicode_ci" "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$DB' AND (engine<>'InnoDB' OR table_collation<>'utf8mb4_unicode_ci')" "0"
eq "nombres de FK con prefijo fk_" "SELECT COUNT(*) FROM information_schema.referential_constraints WHERE constraint_schema='$DB' AND constraint_name NOT LIKE 'fk\\_%'" "0"

echo "== Casos válidos"
ok "usuario sistema (curador del catálogo)" "INSERT INTO usuario (id, firebase_uid, correo, rol, perfil_completo) VALUES (1,'sistema','sistema@cocinasinestres.local','CURADOR',TRUE)"
ok "usuaria nueva con valores por defecto" "INSERT INTO usuario (id, firebase_uid, correo) VALUES (2,'uidA','ana@ejemplo.com')"
eq "rol por defecto USUARIA" "SELECT rol FROM usuario WHERE id=2" "USUARIA"
ok "hogar quincenal 15 y 30" "INSERT INTO hogar (id, usuario_id, presupuesto_monto, periodicidad_ingreso, dia_pago_1, dia_pago_2, dias_sin_tiempo, sin_restricciones) VALUES (1,2,600000,'QUINCENAL',15,30,'[2,4]',TRUE)"
ok "dos personas en el hogar" "INSERT INTO persona (hogar_id, nombre, edad) VALUES (1,'Hijo mayor',12),(1,NULL,35)"
ok "restricciones del catálogo" "INSERT INTO restriccion (id, codigo, nombre, tipo) VALUES (1,'lactosa','Lactosa','ALERGIA'),(2,'gluten','Gluten','ALERGIA'),(3,'azucar','Azúcar (diabetes)','CONDICION')"
ok "producto granel del catálogo (arroz)" "INSERT INTO producto (id, nombre, categoria, tipo_cantidad, unidad_base, unidad_compra, cantidad_unidad_compra, precio_unidad_compra, capacidad_referencia, es_basico, modificado_por) VALUES (1,'Arroz','Granos y básicos','GRANEL','g','libra',500,2600,2500,TRUE,1)"
ok "producto contable del catálogo (leche)" "INSERT INTO producto (id, nombre, categoria, tipo_cantidad, unidad_base, unidad_compra, cantidad_unidad_compra, precio_unidad_compra, modificado_por) VALUES (2,'Leche entera','Lácteos','CONTABLE','ml','bolsa 1 L',1000,4300,1)"
ok "leche contiene lactosa" "INSERT INTO producto_restriccion VALUES (2,1)"
ok "producto adicional del hogar sin precio" "INSERT INTO producto (id, hogar_id, nombre, categoria, tipo_cantidad, unidad_base) VALUES (3,1,'Arepa de la tienda','Otros','CONTABLE','unidad')"
ok "item de despensa estimado por nivel" "INSERT INTO item_despensa (hogar_id, producto_id, cantidad, es_estimada, cantidad_minima, vence_en) VALUES (1,1,1250,TRUE,500,NULL)"
ok "item de despensa contable con vencimiento" "INSERT INTO item_despensa (hogar_id, producto_id, cantidad, vence_en) VALUES (1,2,2000,'2026-10-08')"
ok "item del producto adicional" "INSERT INTO item_despensa (hogar_id, producto_id, cantidad) VALUES (1,3,5)"
ok "receta con ingredientes" "INSERT INTO receta (id, nombre, tiempo_minutos, porciones, pasos, preparacion_previa, modificado_por) VALUES (1,'Arroz con leche',40,4,'1. ...','Remojar el arroz 1 hora',1); INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad) VALUES (1,1,250),(1,2,1000)"
ok "equivalencia del hogar" "INSERT INTO equivalencia_hogar (hogar_id, producto_receta_id, producto_hogar_id) VALUES (1,2,3)"
ok "plan con dos comidas" "INSERT INTO plan_semanal (id, hogar_id, semana_inicio) VALUES (1,1,'2026-10-05'); INSERT INTO comida_plan (plan_semanal_id, dia, tipo_comida, receta_id, id_cambio_cliente) VALUES (1,1,'CENA',1,'c-001'),(1,2,'CENA',1,'c-002')"
ok "lista de compras" "INSERT INTO lista_compras (id, plan_semanal_id) VALUES (1,1); INSERT INTO item_lista_compras (lista_compras_id, producto_id, cantidad, costo_estimado, prioridad) VALUES (1,2,1000,4300,1)"
ok "aviso y preferencia" "INSERT INTO aviso (hogar_id, tipo, mensaje, clave_idempotencia) VALUES (1,'POR_VENCER','La leche vence en 3 días','POR_VENCER:1:2:2026-10-05'); INSERT INTO hogar_preferencia_aviso VALUES (1,'POR_VENCER',TRUE)"
ok "auditoría del catálogo" "INSERT INTO auditoria_catalogo (entidad, entidad_id, accion, usuario_id, cambios) VALUES ('PRODUCTO',1,'CREAR',1,'{\"nombre\":{\"antes\":null,\"despues\":\"Arroz\"}}')"
ok "hogar semanal (viernes)" "INSERT INTO usuario (id, firebase_uid, correo) VALUES (3,'uidB','bea@ejemplo.com'); INSERT INTO hogar (id, usuario_id, presupuesto_monto, periodicidad_ingreso, dia_pago_1) VALUES (2,3,150000,'SEMANAL',5)"

echo "== Restricciones que deben rechazar datos"
ko "firebase_uid duplicado" "INSERT INTO usuario (firebase_uid, correo) VALUES ('uidA','otro@ejemplo.com')"
ko "rol inválido" "INSERT INTO usuario (firebase_uid, correo, rol) VALUES ('uidX','x@x.com','SUPER')"
ko "segundo hogar para la misma usuaria" "INSERT INTO hogar (usuario_id, presupuesto_monto, periodicidad_ingreso, dia_pago_1) VALUES (2,1000,'MENSUAL',1)"
ko "presupuesto en cero" "INSERT INTO usuario (id, firebase_uid, correo) VALUES (9,'uid9','n@n.com'); INSERT INTO hogar (usuario_id, presupuesto_monto, periodicidad_ingreso, dia_pago_1) VALUES (9,0,'MENSUAL',1)"
ko "semanal con día 9" "INSERT INTO hogar (usuario_id, presupuesto_monto, periodicidad_ingreso, dia_pago_1) VALUES (9,1000,'SEMANAL',9)"
ko "quincenal sin segundo día" "INSERT INTO hogar (usuario_id, presupuesto_monto, periodicidad_ingreso, dia_pago_1) VALUES (9,1000,'QUINCENAL',15)"
ko "quincenal con días al revés" "INSERT INTO hogar (usuario_id, presupuesto_monto, periodicidad_ingreso, dia_pago_1, dia_pago_2) VALUES (9,1000,'QUINCENAL',30,15)"
ko "sin restricciones y con restricciones a la vez" "INSERT INTO hogar (usuario_id, presupuesto_monto, periodicidad_ingreso, dia_pago_1, sin_restricciones, restricciones) VALUES (9,1000,'MENSUAL',1,TRUE,'v1:abc')"
ko "edad 150" "INSERT INTO persona (hogar_id, edad) VALUES (1,150)"
ko "granel sin capacidad de referencia" "INSERT INTO producto (nombre, categoria, tipo_cantidad, unidad_base, unidad_compra, cantidad_unidad_compra, precio_unidad_compra, modificado_por) VALUES ('Lenteja','Granos','GRANEL','g','libra',500,4200,1)"
ko "contable con capacidad de referencia" "INSERT INTO producto (nombre, categoria, tipo_cantidad, unidad_base, unidad_compra, cantidad_unidad_compra, precio_unidad_compra, capacidad_referencia, modificado_por) VALUES ('Huevo','Proteínas','CONTABLE','unidad','cubeta',30,15000,30,1)"
ko "producto del catálogo sin precio" "INSERT INTO producto (nombre, categoria, tipo_cantidad, unidad_base, modificado_por) VALUES ('Sal','Condimentos','CONTABLE','g',1)"
ko "producto del catálogo sin curador" "INSERT INTO producto (nombre, categoria, tipo_cantidad, unidad_base, unidad_compra, cantidad_unidad_compra, precio_unidad_compra) VALUES ('Sal','Condimentos','CONTABLE','g','bolsa',500,1500)"
ko "unidad base inválida" "INSERT INTO producto (hogar_id, nombre, categoria, tipo_cantidad, unidad_base) VALUES (1,'X','Otros','CONTABLE','kg')"
ko "mismo producto dos veces en la despensa" "INSERT INTO item_despensa (hogar_id, producto_id, cantidad) VALUES (1,1,100)"
ko "cantidad negativa en despensa" "INSERT INTO item_despensa (hogar_id, producto_id, cantidad) VALUES (2,1,-1)"
ko "ingrediente con cantidad cero" "INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad) VALUES (1,3,0)"
ko "dos comidas en la misma casilla" "INSERT INTO comida_plan (plan_semanal_id, dia, tipo_comida, receta_id) VALUES (1,1,'CENA',1)"
ko "cambio del cliente repetido" "INSERT INTO comida_plan (plan_semanal_id, dia, tipo_comida, receta_id, id_cambio_cliente) VALUES (1,3,'CENA',1,'c-001')"
ko "día 8 en el plan" "INSERT INTO comida_plan (plan_semanal_id, dia, tipo_comida, receta_id) VALUES (1,8,'CENA',1)"
ko "tipo de comida inválido" "INSERT INTO comida_plan (plan_semanal_id, dia, tipo_comida, receta_id) VALUES (1,3,'ONCES',1)"
ko "aviso repetido (idempotencia)" "INSERT INTO aviso (hogar_id, tipo, mensaje, clave_idempotencia) VALUES (1,'POR_VENCER','otra vez','POR_VENCER:1:2:2026-10-05')"
ko "tipo de aviso inválido" "INSERT INTO aviso (hogar_id, tipo, mensaje, clave_idempotencia) VALUES (1,'OTRO','x','k1')"
ko "auditoría sin usuario" "INSERT INTO auditoria_catalogo (entidad, entidad_id, accion) VALUES ('PRODUCTO',1,'EDITAR')"
ko "equivalencia de un producto consigo mismo" "INSERT INTO equivalencia_hogar (hogar_id, producto_receta_id, producto_hogar_id) VALUES (2,1,1)"
ko "borrar un curador con historia en el catálogo" "DELETE FROM usuario WHERE id=1"
ko "borrar un producto usado en una receta" "DELETE FROM producto WHERE id=1"

echo "== Borrado de cuenta (ADR-19): todo el hogar se va en cascada"
ok "borrar la usuaria 2" "DELETE FROM usuario WHERE id=2"
eq "no quedan datos del hogar 1" "SELECT (SELECT COUNT(*) FROM hogar WHERE id=1)+(SELECT COUNT(*) FROM persona)+(SELECT COUNT(*) FROM item_despensa WHERE hogar_id=1)+(SELECT COUNT(*) FROM plan_semanal)+(SELECT COUNT(*) FROM comida_plan)+(SELECT COUNT(*) FROM lista_compras)+(SELECT COUNT(*) FROM item_lista_compras)+(SELECT COUNT(*) FROM aviso)+(SELECT COUNT(*) FROM hogar_preferencia_aviso)+(SELECT COUNT(*) FROM equivalencia_hogar)" "0"
eq "el producto adicional del hogar se borró" "SELECT COUNT(*) FROM producto WHERE id=3" "0"
eq "el catálogo, la receta y la auditoría siguen" "SELECT CONCAT((SELECT COUNT(*) FROM producto WHERE hogar_id IS NULL),'-',(SELECT COUNT(*) FROM receta),'-',(SELECT COUNT(*) FROM ingrediente_receta),'-',(SELECT COUNT(*) FROM auditoria_catalogo))" "2-1-2-1"
eq "el otro hogar no se tocó" "SELECT COUNT(*) FROM hogar WHERE id=2" "1"

echo
if (( fails == 0 )); then echo "RESULTADO: todas las pruebas pasaron"; else echo "RESULTADO: $fails prueba(s) fallaron"; fi

$MYSQL -e "DROP DATABASE IF EXISTS $DB;"
(( fails == 0 ))
