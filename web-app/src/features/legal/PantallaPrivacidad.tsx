import { Link, useNavigate } from 'react-router'
import { MarcaCocina } from '../../shared/componentes'
import './legal.css'

/** Vigencia de esta versión de la política. */
const VIGENTE_DESDE = '26 de octubre de 2026'

/** Correo para ejercer los derechos; se configura en el despliegue (`VITE_CORREO_PRIVACIDAD`). */
const CORREO = (import.meta.env.VITE_CORREO_PRIVACIDAD as string | undefined)?.trim()

/**
 * Política de tratamiento de datos personales (Ley 1581 de 2012). Se abre sin iniciar
 * sesión, desde la bienvenida, el registro, el perfil del hogar y la introducción del
 * Modo cocinar. Dice qué se guarda, para qué, dónde, y qué pasa con la voz y WhatsApp.
 */
export function PantallaPrivacidad() {
  const navegar = useNavigate()
  return (
    <div className="legal">
      <header className="legal__barra">
        <MarcaCocina />
        <button type="button" className="legal__volver" onClick={() => void navegar(-1)}>
          ‹ Volver
        </button>
      </header>
      <main className="legal__cuerpo">
        <h1>Política de tratamiento de datos personales</h1>
        <p className="legal__vigencia">Vigente desde el {VIGENTE_DESDE} · Ley 1581 de 2012</p>

        <h2>Quién responde por tus datos</h2>
        <p>
          El equipo del proyecto académico Cocina sin estrés (Ingeniería de Software III, Universidad del Quindío). La
          aplicación es un prototipo del curso: no vende nada ni comparte tus datos con terceros para publicidad.
        </p>

        <h2>Qué guardamos</h2>
        <ul>
          <li>
            <strong>Tu cuenta:</strong> tu correo. La contraseña la guarda Firebase Authentication (Google); nosotros
            nunca la vemos.
          </li>
          <li>
            <strong>El perfil del hogar:</strong> cuántas personas son y sus edades, el presupuesto, cuándo te pagan y
            los días sin tiempo para cocinar.
          </li>
          <li>
            <strong>Datos de salud:</strong> alergias y condiciones de las personas del hogar, solo si lo autorizas. Se
            guardan cifrados y solo se usan para filtrar y adaptar recetas.
          </li>
          <li>
            <strong>Tu cocina:</strong> la despensa, los planes de la semana, la lista de compras con los precios que
            corriges y las tiendas que guardas para pedir por WhatsApp (nombre y celular de la tienda).
          </li>
          <li>
            <strong>Cifras de uso:</strong> cuándo abres la app, qué recetas cocinas y si usas el modo rescate. Solo se
            usan sumadas, para medir la calidad de la aplicación; no guardan nada de lo que escribes ni de lo que dices.
          </li>
        </ul>

        <h2>Para qué los usamos</h2>
        <p>
          Para armar tu semana con lo que tienes y tu presupuesto, mostrarte recetas que le sirven a tu hogar, avisarte
          lo que vence o se acaba y armar tu lista de compras. Con las cifras de uso, el administrador revisa los
          indicadores del plan de calidad, sin ver los datos de ninguna usuaria.
        </p>

        <h2>La voz del asistente y Manos libres</h2>
        <p>
          El asistente del Modo cocinar habla con la voz de tu celular. <strong>Manos libres</strong> está apagado hasta
          que lo enciendas: usa el reconocimiento de voz de tu navegador. En Chrome, el audio lo procesa el servicio de
          voz de Google para convertirlo en texto. Cocina sin estrés no graba ni guarda el audio: solo entiende «siguiente»,
          «repite», «atrás» y «temporizador». Lo apagas con el botón 🎤.
        </p>

        <h2>Pedir por WhatsApp</h2>
        <p>
          La aplicación solo abre WhatsApp con el mensaje escrito. Tú decides si lo envías; no enviamos mensajes por ti ni
          guardamos conversaciones.
        </p>

        <h2>Dónde están</h2>
        <p>
          La cuenta y la página están en Firebase (Google). La base de datos y el servidor están en Amazon Web Services,
          en Estados Unidos. La aplicación se comunica siempre por una conexión cifrada (HTTPS).
        </p>

        <h2>Tus derechos</h2>
        <p>
          Puedes conocer, actualizar y corregir tus datos, pedir que los borremos y retirar tu autorización. El perfil del
          hogar y la despensa los cambias tú misma desde la aplicación. Para lo demás,{' '}
          {CORREO ? (
            <>
              escríbenos a <a href={`mailto:${CORREO}`}>{CORREO}</a>
            </>
          ) : (
            'escríbele al equipo del proyecto'
          )}
          . Las consultas se atienden en máximo 10 días hábiles y los reclamos en máximo 15, como establece la ley.
        </p>

        <p className="legal__pie">
          <Link to="/">Volver a Cocina sin estrés</Link>
        </p>
      </main>
    </div>
  )
}
