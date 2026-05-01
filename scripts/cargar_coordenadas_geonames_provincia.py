#!/usr/bin/env python3
"""
Script para cargar coordenadas de CAPITALES de municipios con MATCH POR PROVINCIA.

Descarga:
- ES.zip de GeoNames (lugares)
- admin2Codes.txt de GeoNames (mapeo de códigos de provincia)

Usa PPLA3 (capital de municipio) y matchea por:
- Nombre de municipio
- Nombre de provincia (traducido del código admin2 de GeoNames)

Uso:
    python cargar_coordenadas_geonames_provincia.py
"""

import io
import zipfile
import requests
import unicodedata


def limpiar_nombre_provincia(nombre):
    """
    Limpia nombres de provincia de GeoNames.
    GeoNames devuelve 'Province of Córdoba', 'Província de València', etc.
    La BBDD de Festia tiene solo 'Córdoba', 'Valencia', etc.
    """
    if not nombre:
        return ""

    # Casos especiales antes de limpieza general
    especiales = {
        "Araba / Álava": "Álava",
        "Almería": "Almería",
        "Bizkaia": "Bizkaia",
        "Gipuzkoa": "Gipuzkoa",
        "Illes Balears": "Baleares",
        "Murcia": "Murcia",
    }
    if nombre in especiales:
        return especiales[nombre]

    # Quitar prefijos comunes
    prefijos = [
        "Province of ",
        "Provincia de ",
        "Província de ",
        "Província da ",
        "Provincia d'",  # para casos como Cataluña
    ]
    for prefijo in prefijos:
        if nombre.startswith(prefijo):
            nombre = nombre[len(prefijo):]
            break

    return nombre.strip()


def normalizar(texto):
    if not texto:
        return ""
    texto = texto.strip().upper()
    texto = ''.join(
        c for c in unicodedata.normalize('NFD', texto)
        if unicodedata.category(c) != 'Mn'
    )
    return texto


def descargar_admin2_codes():
    """Descarga admin2Codes.txt que mapea códigos de provincia a nombres."""
    url = "http://download.geonames.org/export/dump/admin2Codes.txt"
    print(f"Descargando admin2Codes.txt...")
    r = requests.get(url, timeout=120)
    r.raise_for_status()
    return r.text


def parse_admin2_codes(text):
    """Parsea admin2Codes.txt y devuelve dict {code: province_name_limpio}."""
    mapping = {}
    for line in text.splitlines():
        if not line.strip():
            continue
        parts = line.split("\t")
        if len(parts) >= 2:
            code = parts[0].strip()  # ej: ES.56.PO
            name_raw = parts[1].strip()  # ej: Province of Pontevedra
            # Solo códigos de España
            if code.startswith("ES."):
                name_limpio = limpiar_nombre_provincia(name_raw)
                mapping[code] = name_limpio
    print(f"Mapeadas {len(mapping)} provincias de España.")
    return mapping


def descargar_geonames_es():
    """Descarga ES.zip de GeoNames."""
    url = "http://download.geonames.org/export/dump/ES.zip"
    print(f"Descargando ES.zip de GeoNames...")
    r = requests.get(url, timeout=120)
    r.raise_for_status()
    print(f"Descargados {len(r.content)} bytes.")
    return r.content


def procesar_geonames(data, admin2_map):
    """
    Procesa ES.txt, extrae PPLA3 y PPLA, y les asigna provincia.
    Devuelve dict {(mun_name_norm, prov_name_norm): (lat, lon)}.
    """
    ppla3 = {}  # Capitales de municipio con provincia
    ppla = {}   # Capitales de provincia con provincia
    ppl = {}    # Poblados principales con provincia

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
                admin1_code = cols[10] if len(cols) > 10 else ""
                admin2_code = cols[11] if len(cols) > 11 else ""

                if not lat or not lon:
                    continue

                try:
                    lat_f = float(lat)
                    lon_f = float(lon)
                except ValueError:
                    continue

                # Construir código admin2 completo: ES.{admin1}.{admin2}
                admin2_full = f"ES.{admin1_code}.{admin2_code}" if admin1_code and admin2_code else ""
                province_name = admin2_map.get(admin2_full, "")

                if not province_name:
                    continue

                name_norm = normalizar(name)
                prov_norm = normalizar(province_name)
                key = (name_norm, prov_norm)

                if feature_class == "P" and feature_code == "PPLA3":
                    ppla3[key] = (lat_f, lon_f)
                elif feature_class == "P" and feature_code == "PPLA":
                    if key not in ppla:
                        ppla[key] = (lat_f, lon_f)
                elif feature_class == "P" and feature_code.startswith("PPL"):
                    if key not in ppl:
                        ppl[key] = (lat_f, lon_f)

    print(f"Encontrados:")
    print(f"  {len(ppla3)} PPLA3 (capitales de municipio)")
    print(f"  {len(ppla)} PPLA (capitales de provincia)")
    print(f"  {len(ppl)} PPL (poblados)")

    # Combinar con prioridad
    resultado = dict(ppla3)
    resultado.update({k: v for k, v in ppla.items() if k not in resultado})
    resultado.update({k: v for k, v in ppl.items() if k not in resultado})

    print(f"\nTotal único por municipio+provincia: {len(resultado)}")
    return resultado


def generar_sql(coordenadas):
    """Genera SQL con matching por municipio + provincia."""
    if not coordenadas:
        print("No hay coordenadas para generar SQL.")
        return

    sql_lines = [
        "-- =============================================================================",
        "-- v1.4.2 — Coordenadas de capitales de municipio (GeoNames PPLA3/PPLA)",
        "-- Match por: nombre de municipio + nombre de provincia",
        "-- =============================================================================",
        "",
        "BEGIN;",
        "",
    ]

    for (mun_norm, prov_norm), (lat, lon) in coordenadas.items():
        sql_lines.append(
            f"UPDATE gestmusica.municipio m "
            f"SET latitud = {lat:.8f}, longitud = {lon:.8f} "
            f"FROM gestmusica.provincia p "
            f"WHERE m.id_provincia = p.id "
            f"AND UPPER(UNACCENT(m.nombre)) = '{mun_norm.replace(chr(39), chr(39)+chr(39))}' "
            f"AND UPPER(UNACCENT(p.nombre)) = '{prov_norm.replace(chr(39), chr(39)+chr(39))}';"
        )

    sql_lines.extend([
        "",
        "COMMIT;",
        "",
        "-- Verificación",
        "SELECT COUNT(*) AS municipios_con_coords FROM gestmusica.municipio WHERE latitud IS NOT NULL;",
        "SELECT COUNT(*) AS municipios_sin_coords FROM gestmusica.municipio WHERE latitud IS NULL;",
        "SELECT m.nombre, p.nombre as provincia "
        "FROM gestmusica.municipio m "
        "JOIN gestmusica.provincia p ON m.id_provincia = p.id "
        "WHERE m.latitud IS NULL "
        "ORDER BY p.nombre, m.nombre;",
    ])

    filename = "cargar_coordenadas_geonames_provincia.sql"
    with open(filename, "w", encoding="utf-8") as f:
        f.write("\n".join(sql_lines))

    print(f"\nSQL generado: {filename}")
    print(f"Total sentencias UPDATE: {len(coordenadas)}")


def main():
    admin2_text = descargar_admin2_codes()
    admin2_map = parse_admin2_codes(admin2_text)

    data = descargar_geonames_es()
    coordenadas = procesar_geonames(data, admin2_map)
    generar_sql(coordenadas)

    print("\n========================================")
    print("INSTRUCCIONES:")
    print("========================================")
    print("1. Ejecuta en PostgreSQL:")
    print("   psql -h localhost -U usuario -d base_de_datos -f cargar_coordenadas_geonames_provincia.sql")
    print("2. Verifica que los markers caigan en el casco urbano correcto.")
    print("========================================")


if __name__ == "__main__":
    main()
