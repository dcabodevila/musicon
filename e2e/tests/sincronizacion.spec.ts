import { test } from '@playwright/test';

/*
 * The legacy synchronisation (package sincronizacion, MariaDB/PHP legacy) has no screen or
 * button in the UI: it is only exposed as POST /api/sincronizacion/** (SincronizacionController)
 * and the scheduled SincronizacionJob. Driving it from a browser would mean calling the API
 * directly and writing legacy data into the local database, so it is covered by the unit and
 * @WebMvcTest tests of task T6 instead.
 */
test.describe('Sincronización legacy', () => {
  test('no está expuesta en la interfaz', () => {
    test.skip(true, 'Sin pantalla en la UI: solo API /api/sincronizacion y job programado; cubierta por tests unitarios (T6).');
  });
});
