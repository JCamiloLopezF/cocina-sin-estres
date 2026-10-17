#!/usr/bin/env python3
"""Convierte el reporte XML de JaCoCo en una tabla Markdown para el resumen del job de CI.

Uso: python3 .github/scripts/resumen-jacoco.py backend/build/reports/jacoco/test/jacocoTestReport.xml
"""
import sys
import xml.etree.ElementTree as ET

NOMBRES = {"LINE": "Líneas", "BRANCH": "Ramas (if/else)", "METHOD": "Métodos", "CLASS": "Clases"}


def porcentaje(contador):
    cubiertas = int(contador.get("covered", 0))
    total = cubiertas + int(contador.get("missed", 0))
    return (100.0 * cubiertas / total if total else 100.0), cubiertas, total


def contadores(nodo):
    return {c.get("type"): c for c in nodo.findall("counter")}


def main(ruta):
    try:
        raiz = ET.parse(ruta).getroot()
    except (OSError, ET.ParseError):
        print("### Cobertura del backend\n\nNo se generó el reporte de JaCoCo (las pruebas no llegaron a ejecutarse).")
        return

    print("### Cobertura del backend (JaCoCo)\n")
    print("| Medida | Cobertura | Cubiertas / total |")
    print("| --- | --- | --- |")
    generales = contadores(raiz)
    for tipo, nombre in NOMBRES.items():
        if tipo in generales:
            pct, cub, tot = porcentaje(generales[tipo])
            print(f"| {nombre} | {pct:.1f} % | {cub} / {tot} |")

    paquetes = []
    for paquete in raiz.findall("package"):
        lineas = contadores(paquete).get("LINE")
        if lineas is not None:
            pct, cub, tot = porcentaje(lineas)
            nombre = paquete.get("name", "").replace("/", ".").removeprefix("co.edu.uniquindio.cocinasinestres.")
            paquetes.append((nombre, pct, cub, tot))

    if paquetes:
        print("\n<details><summary>Líneas cubiertas por paquete</summary>\n")
        print("| Paquete | Líneas | Cubiertas / total |")
        print("| --- | --- | --- |")
        for nombre, pct, cub, tot in sorted(paquetes):
            print(f"| `{nombre}` | {pct:.1f} % | {cub} / {tot} |")
        print("\n</details>")

    print("\nEl reporte completo en HTML está en el artefacto **reportes-backend** de esta ejecución.")


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else "build/reports/jacoco/test/jacocoTestReport.xml")
