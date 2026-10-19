import type { RouteObject } from 'react-router'
import { PantallaAdmin } from '../features/admin/PantallaAdmin'
import { PantallaEntrar } from '../features/auth/PantallaEntrar'
import { PantallaRecuperar } from '../features/auth/PantallaRecuperar'
import { PantallaRegistro } from '../features/auth/PantallaRegistro'
import { PantallaCuraduria } from '../features/curaduria/PantallaCuraduria'
import { PantallaAgregarProducto } from '../features/despensa/PantallaAgregarProducto'
import { PantallaDespensa } from '../features/despensa/PantallaDespensa'
import { PantallaItemDespensa } from '../features/despensa/PantallaItemDespensa'
import { PantallaInicio } from '../features/inicio/PantallaInicio'
import { PantallaPerfil } from '../features/perfil/PantallaPerfil'
import { PantallaPerfilHogar } from '../features/perfil/PantallaPerfilHogar'
import { PantallaDia } from '../features/planificador/PantallaDia'
import { PantallaListaCompras } from '../features/planificador/PantallaListaCompras'
import { PantallaPlan } from '../features/planificador/PantallaPlan'
import { PantallaAgregarAlPlan } from '../features/recetario/PantallaAgregarAlPlan'
import { PantallaReceta } from '../features/recetario/PantallaReceta'
import { PantallaRecetario } from '../features/recetario/PantallaRecetario'
import { DisenoOperativo } from './DisenoOperativo'
import { DisenoUsuaria } from './DisenoUsuaria'
import { RequiereRol, RutaPrivada, RutaPublica } from './guardas'
import { PantallaNoEncontrada } from './PantallaNoEncontrada'

/**
 * Mapa de pantallas. Cada rol tiene su propia zona (ADR-16); el backend vuelve a
 * revisar el rol en cada petición, así que esto ordena la navegación pero no es la
 * barrera de seguridad.
 */
export function crearRutas({ modoDev }: { modoDev: boolean }): RouteObject[] {
  return [
    {
      element: <RutaPublica />,
      children: [
        { path: '/entrar', element: <PantallaEntrar modoDev={modoDev} /> },
        { path: '/registro', element: modoDev ? <PantallaEntrar modoDev /> : <PantallaRegistro /> },
        { path: '/recuperar', element: modoDev ? <PantallaEntrar modoDev /> : <PantallaRecuperar /> },
      ],
    },
    {
      element: <RutaPrivada />,
      children: [
        {
          element: <RequiereRol roles={['USUARIA']} />,
          children: [
            {
              element: <DisenoUsuaria />,
              children: [
                { index: true, element: <PantallaInicio /> },
                { path: '/despensa', element: <PantallaDespensa /> },
                { path: '/despensa/agregar', element: <PantallaAgregarProducto /> },
                { path: '/despensa/:id', element: <PantallaItemDespensa /> },
                { path: '/plan', element: <PantallaPlan /> },
                { path: '/plan/dia/:dia', element: <PantallaDia /> },
                { path: '/plan/lista', element: <PantallaListaCompras /> },
                { path: '/recetas', element: <PantallaRecetario /> },
                { path: '/recetas/:id', element: <PantallaReceta /> },
                { path: '/recetas/:id/agregar', element: <PantallaAgregarAlPlan /> },
                { path: '/perfil', element: <PantallaPerfil /> },
                { path: '/perfil/hogar', element: <PantallaPerfilHogar /> },
              ],
            },
          ],
        },
        {
          element: <RequiereRol roles={['CURADOR']} />,
          children: [{ element: <DisenoOperativo />, children: [{ path: '/curaduria', element: <PantallaCuraduria /> }] }],
        },
        {
          element: <RequiereRol roles={['ADMIN']} />,
          children: [{ element: <DisenoOperativo />, children: [{ path: '/admin', element: <PantallaAdmin /> }] }],
        },
      ],
    },
    { path: '*', element: <PantallaNoEncontrada /> },
  ]
}
