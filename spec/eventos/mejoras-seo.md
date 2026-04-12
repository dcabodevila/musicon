# Mejoras SEO a implementar en /eventos

Necesitamos mejorar el posicionamiento de eventos en google, para ello el primer paso es mejorar cómo se gestionan los filters de provincia y municipio en las distintas páginas ya que actualmente sale siempre un listado de miles de municipios y Google penaliza.

Propongo la siguiente solución:

- Inicialmente el select de municipios sin resultados, con una option vacía que ponga "Selecciona provincia".
- Al seleccionar provincia, cargar el select de municipios.
- Al cargar las páginas de /eventos/provincia/lugo -> Si existe, el select de provincia aparece con dicha provincia seleccionada y cargamos los municipios de dicha provincia en el select.
En el hero, donde pone Próximas actuaciones en [provincia] debe poner el nombre de la provincia de la bbdd, no lo que va en la url, ya que debe mostrar las mayúsculas y minúsculas adecuadas.
Existen implementaciones en comun.js para cargar provincias y municipios, revisa la implementación y utilízala si es viable.
- Añadir un info antes de los resultados en el que se especifique: "Actuaciones musicales confirmadas en los próximos 45 días"
 

