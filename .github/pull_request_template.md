## Tarea de Jira

KAN-

## Qué cambia

<!-- Dos o tres líneas: qué hace este PR y por qué. -->

## Cómo probarlo

<!-- Pasos para que quien revisa lo verifique. Ejemplo:
1. Iniciar sesión con una usuaria de prueba.
2. Ir a Despensa → Por vencer.
3. Ver que aparecen solo productos que vencen en 7 días o menos. -->

## RNF relacionado

<!-- Ejemplo: RNF-13 (confirmación al eliminar). Escribe "Ninguno" si no aplica. -->

## Capturas

<!-- Solo si cambia la interfaz. Arrastra las imágenes aquí. -->

## Definición de Terminado

- [ ] El título del PR sigue el formato `tipo(módulo): KAN-xx descripción`
- [ ] Incluye pruebas unitarias de la lógica nueva (sobre todo en `domain/` y en los servicios)
- [ ] Cumple el criterio de aceptación del RNF relacionado
- [ ] Respeta `CONTRIBUTING.md` y reutiliza componentes de `shared/` (RNF-30)
- [ ] No incluye secretos, archivos `.env` ni datos reales de personas
- [ ] Si cambia el esquema, agrega una migración nueva (no edita una existente)
- [ ] Actualicé el README del módulo si cambió cómo se ejecuta o configura
- [ ] Después del merge: desplegado en el ambiente de desarrollo y tarea movida en Jira
