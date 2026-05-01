#!/usr/bin/env python3
"""
Script para descargar y cargar coordenadas de CAPITALES de municipios españoles desde GeoNames.

GeoNames tiene un campo 'feature_code' que distingue:
- PPLA3 = capital de municipio (el casco urbano real)
- PPLA2 = capital de provincia  
- PPL = lugar poblado principal
- ADM3 = división administrativa (municipio)

Usamos PPLA3 como prioridad, y si no existe, buscamos PPL del mismo nombre.

Uso:
    python cargar_coordenadas_geonames_capitales.py

Requiere: requests (pip install requests)
"""

import io
import requests
import unicodedata


def descargar_geonames():
    """Descarga ES.zip de GeoNames."""
    url = "http://download.geonames.org/export/dump/ES.zip"
    print(f"Descargando GeoNames España...")
    r = requests.get(url, timeout=120)
    r.raise_for_status()
    print(f"Descargados {len(r.content)} bytes.")
    return r.content


def normalizar(texto):
    """Normaliza texto para matching."""
    if not texto:
        return ""
    texto = texto.strip().upper()
    texto = ''.join(
        c for c in unicodedata.normalize('NFD', texto)
        if unicodedata.category(c) != 'Mn'
    )
    return texto


def procesar_geonames(data):
    """Procesa ES.txt de GeoNames, priorizando PPLA3 (capitales de municipio)."""
    import zipfile

    # Fase 1: Extraer todas las entradas
    ppla3 = {}      # Capitales de municipio
    ppla = {}       # Capitales de provincia
    ppl = {}        # Lugares poblados
    adm3 = {}       # Divisiones administrativas (municipio)

    with zipfile.ZipFile(io.BytesIO(data)) as z:
        with z.open("ES.txt") as f:
            for line in f:
                line = line.decode("utf-8").strip()
                if not line:
                    continue

                cols = line.split("\t")
                if len(cols) < 19:
                    continue

                name = cols[1].strip()
                lat = cols[4]
                lon = cols[5]
                feature_class = cols[6]
                feature_code = cols[7]
                admin1 = cols[10]  # código de provincia/comunidad
                admin2 = cols[11]  # código de provincia
                admin3 = cols[12]  # código de municipio

                if not lat or not lon:
                    continue

                try:
                    lat_f = float(lat)
                    lon_f = float(lon)
                except ValueError:
                    continue

                name_norm = normalizar(name)
                key = name_norm

                if feature_class == "P" and feature_code == "PPLA3":
                    ppla3[key] = (lat_f, lon_f, name)
                elif feature_class == "P" and feature_code == "PPLA":
                    ppla[key] = (lat_f, lon_f, name)
                elif feature_class == "P" and feature_code.startswith("PPL"):
                    if key not in ppl:
                        ppl[key] = (lat_f, lon_f, name)
                elif feature_class == "A" and feature_code == "ADM3":
                    if key not in adm3:
                        adm3[key] = (lat_f, lon_f, name)

    print(f"Encontrados:")
    print(f"  {len(ppla3)} capitales de municipio (PPLA3)")
    print(f"  {len(ppla)} capitales de provincia (PPLA)")
    print(f"  {len(ppl)} lugares poblados (PPL*)")
    print(f"  {len(adm3)} divisiones administrativas (ADM3)")

    # Fase 2: Combinar con prioridad
    resultado = {}

    # Prioridad 1: PPLA3 (capitales de municipio)
    for key, (lat, lon, name) in ppla3.items():
        resultado[key] = (lat, lon)

    # Prioridad 2: PPLA (capitales de provincia, para municipios que son capital)
    for key, (lat, lon, name) in ppla.items():
        if key not in resultado:
            resultado[key] = (lat, lon)

    # Prioridad 3: PPL (poblado principal)
    for key, (lat, lon, name) in ppl.items():
        if key not in resultado:
            resultado[key] = (lat, lon)

    # Prioridad 4: ADM3 (centroide del municipio, último recurso)
    for key, (lat, lon, name) in adm3.items():
        if key not in resultado:
            resultado[key] = (lat, lon)

    print(f"\nTotal combinado: {len(resultado)} municipios con coordenadas.")
    return resultado


def generar_sql(coordenadas):
    """Genera SQL con matching por nombre de municipio."""
    if not coordenadas:
        print("No hay coordenadas para generar SQL.")
        return

    sql_lines = [
        "-- =============================================================================",
        "-- v1.4.2 — Carga de coordenadas de CAPITALES de municipios (GeoNames PPLA3/PPLA)",
        "-- Prioridad: PPLA3 > PPLA > PPL > ADM3 (casco urbano real, no centroide)",
        "-- =============================================================================",
        "",
        "BEGIN;",
        "",
    ]

    for nombre_norm, (lat, lon) in coordenadas.items():
        sql_lines.append(
            f"UPDATE gestmusica.municipio "
            f"SET latitud = {lat:.8f}, longitud = {lon:.8f} "
            f"WHERE UPPER(UNACCENT(nombre)) = '{nombre_norm.replace(chr(39), chr(39)+chr(39))}' "
            f"AND latitud IS NULL;"
        )

    sql_lines.extend([
        "",
        "COMMIT;",
        "",
        "-- =============================================================================",
        "-- Verificación",
        "-- =============================================================================",
        "SELECT COUNT(*) AS municipios_con_coords FROM gestmusica.municipio WHERE latitud IS NOT NULL;",
        "SELECT COUNT(*) AS municipios_sin_coords FROM gestmusica.municipio WHERE latitud IS NULL;",
        "SELECT m.nombre, p.nombre as provincia "
        "FROM gestmusica.municipio m "
        "JOIN gestmusica.provincia p ON m.id_provincia = p.id "
        "WHERE m.latitud IS NULL "
        "ORDER BY p.nombre, m.nombre;",
    ])

    filename = "cargar_coordenadas_capitales_geonames.sql"
    with open(filename, "w", encoding="utf-8") as f:
        f.write("\n".join(sql_lines))

    print(f"SQL generado: {filename}")
    print(f"Total de sentencias UPDATE: {len(coordenadas)}")


def main():
    data = descargar_geonames()
    coordenadas = procesar_geonames(data)
    generar_sql(coordenadas)

    print("\n========================================")
    print("INSTRUCCIONES:")
    print("========================================")
    print("1. Asegúrate de que PostgreSQL tenga 'unaccent':")
    print("   CREATE EXTENSION IF NOT EXISTS unaccent;")
    print("2. Ejecuta en tu base de datos:")
    print("   psql -h localhost -U usuario -d base_de_datos -f cargar_coordenadas_capitales_geonames.sql")
    print("3. Verifica que los markers ahora apunten al casco urbano real.")
    print("========================================")


if __name__ == "__main__":
    main()
