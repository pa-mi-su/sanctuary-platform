import Foundation
import SwiftUI
import Combine

enum AppLanguage: String, CaseIterable, Identifiable {
    case en
    case es
    case pl

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .en: return "English"
        case .es: return "Español"
        case .pl: return "Polski"
        }
    }

    var contentLocale: ContentLocale {
        switch self {
        case .en: return .en
        case .es: return .es
        case .pl: return .pl
        }
    }

    var locale: Locale {
        switch self {
        case .en: return Locale(identifier: "en_US")
        case .es: return Locale(identifier: "es_ES")
        case .pl: return Locale(identifier: "pl_PL")
        }
    }

    var dailyReadingsLandingURL: URL {
        switch self {
        case .es:
            return URL(string: "https://bible.usccb.org/es/daily-bible-reading")!
        case .en, .pl:
            return URL(string: "https://bible.usccb.org/daily-bible-reading")!
        }
    }

    func localizedDailyReadingsURL(from rawURL: String?) -> URL? {
        guard let rawURL, let url = URL(string: rawURL) else {
            return dailyReadingsLandingURL
        }

        guard self == .es else { return url }

        let value = rawURL
        if value.contains("/es/") {
            return url
        }

        if value.hasPrefix("https://bible.usccb.org/bible/readings/") {
            let translated = value.replacingOccurrences(
                of: "https://bible.usccb.org/bible/readings/",
                with: "https://bible.usccb.org/es/bible/lecturas/"
            )
            return URL(string: translated) ?? url
        }

        if value == "https://bible.usccb.org/daily-bible-reading" {
            return dailyReadingsLandingURL
        }

        if value == "https://bible.usccb.org/" {
            return dailyReadingsLandingURL
        }

        return url
    }
}

@MainActor
final class LocalizationManager: ObservableObject {
    @Published var language: AppLanguage = .en

    func t(_ key: String) -> String {
        switch language {
        case .en: return LocalizationManager.english[key] ?? key
        case .es: return LocalizationManager.spanish[key] ?? LocalizationManager.english[key] ?? key
        case .pl: return LocalizationManager.polish[key] ?? LocalizationManager.english[key] ?? key
        }
    }

    func weekdaySymbolsShort() -> [String] {
        [
            t("weekday.sun"),
            t("weekday.mon"),
            t("weekday.tue"),
            t("weekday.wed"),
            t("weekday.thu"),
            t("weekday.fri"),
            t("weekday.sat")
        ]
    }

    func formatMonthDay(month: Int, day: Int, year: Int? = nil) -> String {
        let resolvedYear = year ?? Calendar.autoupdatingCurrent.component(.year, from: Date())
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = .autoupdatingCurrent
        var components = DateComponents()
        components.calendar = calendar
        components.timeZone = calendar.timeZone
        components.year = resolvedYear
        components.month = month
        components.day = day
        components.hour = 12
        guard let date = calendar.date(from: components) else {
            return String(format: "%02d-%02d", month, day)
        }
        return formatMonthDay(date)
    }

    func formatMonthDay(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.locale = language.locale
        formatter.timeZone = .autoupdatingCurrent
        formatter.setLocalizedDateFormatFromTemplate("MMM d")
        return formatter.string(from: date)
    }

    func monthName(_ month: Int) -> String {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = .autoupdatingCurrent
        var components = DateComponents()
        components.calendar = calendar
        components.timeZone = calendar.timeZone
        components.year = 2026
        components.month = month
        components.day = 1
        guard let date = calendar.date(from: components) else {
            return String(month)
        }

        let formatter = DateFormatter()
        formatter.locale = language.locale
        formatter.timeZone = .autoupdatingCurrent
        formatter.setLocalizedDateFormatFromTemplate("MMMM")
        return formatter.string(from: date)
    }

    func formatMonthYear(month: Int, year: Int) -> String {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = .autoupdatingCurrent
        var components = DateComponents()
        components.calendar = calendar
        components.timeZone = calendar.timeZone
        components.year = year
        components.month = month
        components.day = 1
        components.hour = 12
        guard let date = calendar.date(from: components) else {
            return "\(monthName(month)) \(year)"
        }

        let formatter = DateFormatter()
        formatter.locale = language.locale
        formatter.timeZone = .autoupdatingCurrent
        formatter.setLocalizedDateFormatFromTemplate("MMMM y")
        return formatter.string(from: date)
    }

    func formatMonthDayYear(month: Int, day: Int, year: Int) -> String {
        let resolvedYear = year
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = .autoupdatingCurrent
        var components = DateComponents()
        components.calendar = calendar
        components.timeZone = calendar.timeZone
        components.year = resolvedYear
        components.month = month
        components.day = day
        components.hour = 12
        guard let date = calendar.date(from: components) else {
            return "\(monthName(month)) \(day), \(year)"
        }

        let formatter = DateFormatter()
        formatter.locale = language.locale
        formatter.timeZone = .autoupdatingCurrent
        formatter.setLocalizedDateFormatFromTemplate("MMMM d y")
        return formatter.string(from: date)
    }

    private static let english: [String: String] = [
        "tab.home": "Home",
        "tab.novenas": "Novenas",
        "tab.liturgical": "Liturgical",
        "tab.saints": "Saints",
        "tab.me": "Me",
        "common.on": "On",
        "common.off": "Off",
        "home.about": "About Sanctuary",
        "home.language": "Language",
        "home.welcome": "Welcome to your sanctuary",
        "home.connect": "How do you want to connect with God?",
        "home.supporting": "Prayer, liturgy, and saints in one calm place.",
        "home.eyebrow": "#1 Catholic Prayer Companion",
        "home.saints": "Saints",
        "home.saintsSubtitle": "Feasts and biographies",
        "home.novenasSubtitle": "Journeys of prayer",
        "home.prayers": "Prayers",
        "home.prayersSubtitle": "Daily essentials",
        "home.daily": "Daily Readings",
        "home.dailySubtitle": "Readings and seasons",
        "home.intentions": "Intentions",
        "home.intentionsSubtitle": "Search by need",
        "home.parish": "Find My Local Parish",
        "parish.title": "Find My Local Parish",
        "parish.subtitle": "Find the closest Catholic parish near your current location.",
        "parish.findButton": "Find Nearest Catholic Parish",
        "parish.searching": "Searching for the closest parish...",
        "parish.searchingDetail": "Please be patient. This can take a minute or so while we check nearby Catholic parishes.",
        "parish.distance": "Distance",
        "parish.openMaps": "Open in Maps",
        "parish.website": "Parish Website",
        "parish.error.locationDenied": "Location access is required to find nearby Catholic parishes. Please enable location permissions in Settings.",
        "parish.error.noLocation": "Could not determine your location.",
        "parish.error.noneFound": "No nearby Catholic parish found.",
        "parish.error.generic": "Unable to find a nearby parish right now.",
        "common.loading": "Loading Sanctuary...",
        "common.loadingDetail": "Please wait a moment while your content comes into view.",
        "home.chooseLanguage": "Choose language",
        "common.close": "Close",
        "about.title": "Sanctuary",
        "about.heroTitle": "About Sanctuary",
        "about.subtitle": "Sanctuary is a Catholic companion for prayer, daily readings, saints, liturgical living, and novenas.",
        "about.desktopVersion": "Use Sanctuary on desktop",
        "about.desktopBody": "You can also use Sanctuary on desktop for the full web experience, including daily readings, saints, liturgical browsing, novenas, and your synced account.",
        "about.versionTitle": "App version",
        "about.versionLabel": "Version",
        "about.buildLabel": "Build",
        "about.environmentLabel": "Environment",
        "about.whatsInApp": "What's in the app",
        "about.references": "References",
        "about.contact": "Contact & feedback",
        "about.item.liturgical": "• Liturgical: day, week, and month calendar views with season context and direct daily readings links.",
        "about.item.saints": "• Saints: date-aware saint listings, detailed profiles, and searchable content.",
        "about.item.novenas": "• Novenas: rule-based start dates, end-date tracking, intentions search, and progress management.",
        "about.refsIntro": "Sanctuary currently references these public sources for readings and saint information.",
        "about.source.usccb": "• USCCB (daily readings)",
        "about.source.wikipedia": "• Wikipedia",
        "about.link.usccb": "USCCB Daily Bible Reading",
        "about.link.wikipedia": "Wikipedia",
        "about.contactBody": "To report bugs, request corrections, or send general comments, contact the app team directly.",
        "about.contactBodyDetailed": "To report bugs, request corrections, or send feedback, contact us and include the page or feature you were using along with a short description of the issue.",
        "about.link.desktop": "Open mydailysanctuary.com",
        "about.link.emailSupport": "Email Support",
        "about.link.support": "Support",
        "about.link.privacy": "Privacy Policy",
        "about.link.reportBug": "Report a bug",
        "about.link.feedback": "Send feedback",
        "about.copyright": "Sanctuary © 2026. All rights reserved.",
        "about.privacyEffectiveDate": "Effective date: April 13, 2026",
        "about.support.helpTitle": "Help and Feedback",
        "about.support.helpBody": "If you need help with Sanctuary, have a bug to report, or want to suggest an improvement, contact us at info@mydailysanctuary.com and include your device type, platform, and a short description of the issue.",
        "about.support.featuresTitle": "App Features",
        "about.support.featuresBody": "Sanctuary includes Catholic prayers, novenas, saint reflections, liturgical calendar content, and optional reminders to support prayer throughout the day.",
        "about.support.responseTitle": "Response Time",
        "about.support.responseBody": "We do our best to respond to support requests promptly.",
        "about.privacy.collectTitle": "Information We Collect",
        "about.privacy.collectBody": "Sanctuary is designed to work primarily with local content on your device. We do not require account creation to use the app.",
        "about.privacy.locationTitle": "Location Data",
        "about.privacy.locationBody": "If you choose to allow location access, Sanctuary uses your location to help find nearby parishes. Location access is optional and can be changed at any time in your device settings.",
        "about.privacy.notificationsTitle": "Notifications",
        "about.privacy.notificationsBody": "If you choose to allow notifications, Sanctuary uses notification permissions to send reminder notifications for prayer and novena activity. Notifications are optional and can be disabled at any time in your device settings.",
        "about.privacy.sharingTitle": "Data Sharing",
        "about.privacy.sharingBody": "We do not sell your personal information.",
        "about.privacy.choicesTitle": "Your Choices",
        "about.privacy.choicesBody": "You can deny location and notification permissions and still use the core app experience.",
        "about.privacy.contactTitle": "Contact",
        "about.privacy.contactBody": "If you have privacy questions, contact info@mydailysanctuary.com.",
        "calendar.today": "Today",
        "calendar.day": "Day",
        "calendar.week": "Week",
        "calendar.month": "Month",
        "calendar.search": "Search",
        "calendar.searchSaints": "Search Saints",
        "calendar.searchNovenas": "Search Novenas",
        "calendar.searchIntentions": "Search Novena Intentions",
        "calendar.subtitle.novenas": "Novenas • Tap to jump",
        "calendar.subtitle.liturgical": "Liturgical • Tap to jump",
        "calendar.subtitle.saints": "Saints • Tap to jump",
        "calendar.noNovenaMapped": "No novena mapped for",
        "calendar.noSaintMapped": "No saint mapped for",
        "calendar.noNovenaAvailable": "No novena available",
        "calendar.dailyReadings": "Daily Readings",
        "calendar.noEntry": "No Entry",
        "calendar.ok": "OK",
        "calendar.pickDate": "Pick Date",
        "calendar.openDailyReadings": "Open daily readings",
        "calendar.openDetails": "Open details",
        "season.advent": "Advent",
        "season.christmas": "Christmas",
        "season.lent": "Lent",
        "season.easter": "Easter",
        "season.ordinary": "Ordinary Time",
        "weekday.sun": "Sun",
        "weekday.mon": "Mon",
        "weekday.tue": "Tue",
        "weekday.wed": "Wed",
        "weekday.thu": "Thu",
        "weekday.fri": "Fri",
        "weekday.sat": "Sat",
        "search.saintsTitle": "Search Saints",
        "search.novenasTitle": "Search Novenas",
        "search.prayersTitle": "Search Prayers",
        "search.saintsPrompt": "Search saints",
        "search.novenasPrompt": "Search novenas",
        "search.intentionsPrompt": "Search intentions (e.g. moms, healing, anxiety...)",
        "search.novenaType": "Novena",
        "search.intentionsLabel": "Intentions",
        "search.prayersPrompt": "Search prayers...",
        "search.results": "results",
        "common.cancel": "Cancel",
        "common.apply": "Apply",
        "common.done": "Done",
        "common.pickDateLabel": "Select Date",
        "common.closeUpper": "Close",
        "saints.feastShort": "Feast",
        "detail.feastDate": "Feast date",
        "detail.endDate": "End date",
        "detail.novenaStartDate": "Novena start date",
        "detail.novenaEndDate": "Novena end date",
        "detail.summary": "Summary",
        "detail.biography": "Biography",
        "detail.patronages": "Patronages",
        "detail.prayers": "Prayers",
        "detail.sources": "Sources",
        "detail.relatedNovenas": "Related Novenas",
        "detail.relatedSaints": "Related Saints",
        "detail.addFavorites": "Add to Favorites",
        "detail.savedFavorites": "Saved to Favorites",
        "detail.note": "Note",
        "novena.chooseDay": "Choose a day",
        "novena.dayLabel": "Day",
        "novena.noDayContent": "No day content found for this novena.",
        "novena.scripture": "Scripture",
        "novena.prayer": "Prayer",
        "novena.reflection": "Reflection",
        "novena.loginPrompt": "Log in or register to start this novena and track your progress.",
        "novena.start": "Start Novena",
        "novena.stop": "Stop Novena",
        "novena.completeDay": "Complete Day",
        "novena.completed": "Novena Completed",
        "novena.completedTitle": "Novena Completed",
        "novena.completedMessagePrefix": "Thank you for completing the",
        "novena.completedMessageSuffix": "novena.",
        "me.noneInProgress": "No novenas in progress.",
        "me.noneFavoriteNovenas": "No favorite novenas yet.",
        "me.noneFavoriteSaints": "No favorite saints yet.",
        "me.subtitle": "Your novenas in progress and saved favorites.",
        "me.signedIn": "Signed in",
        "me.identitySupport": "Your favorites, active novenas, and future account settings live here.",
        "me.logout": "Logout",
        "me.fallbackName": "Sanctuary member",
        "me.inProgress": "Novenas in Progress",
        "me.favoriteNovenas": "Favorite Novenas",
        "me.favoriteSaints": "Favorite Saints",
        "me.reminders": "Reminders",
        "me.reminders.inProgressTitle": "Novenas in progress",
        "me.reminders.inProgressBody": "Send morning and evening reminders when you have a novena in progress.",
        "me.reminders.generalTitle": "Once-daily Sanctuary reminder",
        "me.reminders.generalBody": "Send a gentle morning reminder when you do not have a novena in progress."
    ]

    private static let spanish: [String: String] = [
        "tab.home": "Inicio",
        "tab.novenas": "Novenas",
        "tab.liturgical": "Litúrgico",
        "tab.saints": "Santos",
        "tab.me": "Yo",
        "common.on": "Activado",
        "common.off": "Desactivado",
        "home.about": "Acerca de Sanctuary",
        "home.language": "Idioma",
        "home.welcome": "Bienvenido a tu santuario",
        "home.connect": "¿Cómo quieres conectarte con Dios?",
        "home.supporting": "Oración, liturgia y santos en un lugar sereno.",
        "home.eyebrow": "#1 Compañero católico de oración",
        "home.saints": "Santos",
        "home.saintsSubtitle": "Fiestas y biografías",
        "home.novenasSubtitle": "Jornadas de oración",
        "home.prayers": "Oraciones",
        "home.prayersSubtitle": "Esenciales diarios",
        "home.daily": "Lecturas diarias",
        "home.dailySubtitle": "Lecturas y tiempos",
        "home.intentions": "Intenciones",
        "home.intentionsSubtitle": "Buscar por necesidad",
        "home.parish": "Encontrar mi parroquia local",
        "parish.title": "Encontrar mi parroquia local",
        "parish.subtitle": "Encuentra la parroquia católica más cercana a tu ubicación actual.",
        "parish.findButton": "Buscar parroquia católica más cercana",
        "parish.distance": "Distancia",
        "parish.openMaps": "Abrir en Mapas",
        "parish.website": "Sitio web de la parroquia",
        "parish.searching": "Buscando la parroquia más cercana...",
        "parish.searchingDetail": "Por favor, ten paciencia. Esto puede tardar un minuto más o menos mientras revisamos las parroquias católicas cercanas.",
        "parish.error.locationDenied": "Se requiere acceso a la ubicación para encontrar parroquias católicas cercanas. Actívalo en Configuración.",
        "parish.error.noLocation": "No se pudo determinar tu ubicación.",
        "parish.error.noneFound": "No se encontró una parroquia católica cercana.",
        "parish.error.generic": "No se pudo encontrar una parroquia cercana en este momento.",
        "common.loading": "Cargando Sanctuary...",
        "common.loadingDetail": "Espera un momento mientras aparece tu contenido.",
        "home.chooseLanguage": "Elegir idioma",
        "common.close": "Cerrar",
        "about.title": "Sanctuary",
        "about.heroTitle": "Acerca de Sanctuary",
        "about.subtitle": "Sanctuary es un acompañante católico para la oración, las lecturas diarias, los santos, la vida litúrgica y las novenas.",
        "about.desktopVersion": "Usa Sanctuary en escritorio",
        "about.desktopBody": "También puedes usar Sanctuary en escritorio para la experiencia web completa, incluidas lecturas diarias, santos, navegación litúrgica, novenas y tu cuenta sincronizada.",
        "about.versionTitle": "Versión de la app",
        "about.versionLabel": "Versión",
        "about.buildLabel": "Compilación",
        "about.environmentLabel": "Entorno",
        "about.whatsInApp": "Qué incluye Sanctuary",
        "about.references": "Referencias",
        "about.contact": "Contacto y comentarios",
        "about.item.liturgical": "• Litúrgico: vistas por día, semana y mes con contexto de temporada y acceso directo a lecturas diarias.",
        "about.item.saints": "• Santos: listados por fecha, perfiles detallados y búsqueda de contenido.",
        "about.item.novenas": "• Novenas: fechas de inicio basadas en reglas, seguimiento de fecha de finalización, búsqueda de intenciones y progreso.",
        "about.refsIntro": "Sanctuary utiliza actualmente estas fuentes públicas para lecturas e información de santos.",
        "about.source.usccb": "• USCCB (lecturas diarias)",
        "about.source.wikipedia": "• Wikipedia",
        "about.link.usccb": "Lecturas diarias de la USCCB",
        "about.link.wikipedia": "Wikipedia",
        "about.contactBody": "Para reportar errores, solicitar correcciones o enviar comentarios generales, contacta directamente al equipo de la app.",
        "about.contactBodyDetailed": "Para reportar errores, solicitar correcciones o enviar comentarios, contáctanos e incluye la página o función que estabas usando junto con una breve descripción del problema.",
        "about.link.desktop": "Abrir mydailysanctuary.com",
        "about.link.emailSupport": "Escribir a soporte",
        "about.link.support": "Soporte",
        "about.link.privacy": "Política de privacidad",
        "about.link.reportBug": "Reportar un error",
        "about.link.feedback": "Enviar comentarios",
        "about.copyright": "Sanctuary © 2026. Todos los derechos reservados.",
        "about.privacyEffectiveDate": "Fecha de vigencia: 13 de abril de 2026",
        "about.support.helpTitle": "Ayuda y comentarios",
        "about.support.helpBody": "Si necesitas ayuda con Sanctuary, quieres reportar un error o sugerir una mejora, contáctanos en info@mydailysanctuary.com e incluye tu dispositivo, plataforma y una breve descripción del problema.",
        "about.support.featuresTitle": "Funciones de la app",
        "about.support.featuresBody": "Sanctuary incluye oraciones católicas, novenas, reflexiones de santos, contenido del calendario litúrgico y recordatorios opcionales para acompañar la oración durante el día.",
        "about.support.responseTitle": "Tiempo de respuesta",
        "about.support.responseBody": "Hacemos todo lo posible por responder rápidamente a las solicitudes de soporte.",
        "about.privacy.collectTitle": "Información que recopilamos",
        "about.privacy.collectBody": "Sanctuary está diseñado para funcionar principalmente con contenido local en tu dispositivo. No exigimos crear una cuenta para usar la app.",
        "about.privacy.locationTitle": "Datos de ubicación",
        "about.privacy.locationBody": "Si decides permitir el acceso a tu ubicación, Sanctuary la usa para ayudarte a encontrar parroquias cercanas. El acceso a la ubicación es opcional y puedes cambiarlo en cualquier momento en la configuración de tu dispositivo.",
        "about.privacy.notificationsTitle": "Notificaciones",
        "about.privacy.notificationsBody": "Si decides permitir las notificaciones, Sanctuary usa esos permisos para enviar recordatorios sobre la oración y la actividad de novenas. Las notificaciones son opcionales y pueden desactivarse en cualquier momento desde la configuración de tu dispositivo.",
        "about.privacy.sharingTitle": "Intercambio de datos",
        "about.privacy.sharingBody": "No vendemos tu información personal.",
        "about.privacy.choicesTitle": "Tus opciones",
        "about.privacy.choicesBody": "Puedes rechazar los permisos de ubicación y notificaciones y seguir usando la experiencia principal de la app.",
        "about.privacy.contactTitle": "Contacto",
        "about.privacy.contactBody": "Si tienes preguntas sobre privacidad, contacta con info@mydailysanctuary.com.",
        "calendar.today": "Hoy",
        "calendar.day": "Día",
        "calendar.week": "Semana",
        "calendar.month": "Mes",
        "calendar.search": "Buscar",
        "calendar.searchSaints": "Buscar santos",
        "calendar.searchNovenas": "Buscar novenas",
        "calendar.searchIntentions": "Buscar intenciones de novena",
        "calendar.subtitle.novenas": "Novenas • Toca para saltar",
        "calendar.subtitle.liturgical": "Litúrgico • Toca para saltar",
        "calendar.subtitle.saints": "Santos • Toca para saltar",
        "calendar.noNovenaMapped": "No hay novena asignada para",
        "calendar.noSaintMapped": "No hay santo asignado para",
        "calendar.noNovenaAvailable": "No hay novena disponible",
        "calendar.dailyReadings": "Lecturas diarias",
        "calendar.noEntry": "Sin entrada",
        "calendar.ok": "OK",
        "calendar.pickDate": "Elegir fecha",
        "calendar.openDailyReadings": "Abrir lecturas diarias",
        "calendar.openDetails": "Abrir detalles",
        "season.advent": "Adviento",
        "season.christmas": "Navidad",
        "season.lent": "Cuaresma",
        "season.easter": "Pascua",
        "season.ordinary": "Tiempo ordinario",
        "weekday.sun": "Dom",
        "weekday.mon": "Lun",
        "weekday.tue": "Mar",
        "weekday.wed": "Mié",
        "weekday.thu": "Jue",
        "weekday.fri": "Vie",
        "weekday.sat": "Sáb",
        "search.saintsTitle": "Buscar santos",
        "search.novenasTitle": "Buscar novenas",
        "search.prayersTitle": "Buscar oraciones",
        "search.saintsPrompt": "Buscar santos",
        "search.novenasPrompt": "Buscar novenas",
        "search.intentionsPrompt": "Buscar intenciones (p. ej., mamás, sanación, ansiedad...)",
        "search.novenaType": "Novena",
        "search.intentionsLabel": "Intenciones",
        "search.prayersPrompt": "Buscar oraciones...",
        "search.results": "resultados",
        "common.cancel": "Cancelar",
        "common.apply": "Aplicar",
        "common.done": "Listo",
        "common.pickDateLabel": "Seleccionar fecha",
        "common.closeUpper": "Cerrar",
        "saints.feastShort": "Fiesta",
        "detail.feastDate": "Fecha de fiesta",
        "detail.endDate": "Fecha de finalización",
        "detail.novenaStartDate": "Fecha de inicio de la novena",
        "detail.novenaEndDate": "Fecha de finalización de la novena",
        "detail.summary": "Resumen",
        "detail.biography": "Biografía",
        "detail.patronages": "Patrocinios",
        "detail.prayers": "Oraciones",
        "detail.sources": "Fuentes",
        "detail.relatedNovenas": "Novenas relacionadas",
        "detail.relatedSaints": "Santos relacionados",
        "detail.addFavorites": "Agregar a favoritos",
        "detail.savedFavorites": "Guardado en favoritos",
        "detail.note": "Nota",
        "novena.chooseDay": "Elige un día",
        "novena.dayLabel": "Día",
        "novena.noDayContent": "No se encontró contenido para este día de la novena.",
        "novena.scripture": "Escritura",
        "novena.prayer": "Oración",
        "novena.reflection": "Reflexión",
        "novena.loginPrompt": "Inicia sesión o regístrate para comenzar esta novena y seguir tu progreso.",
        "novena.start": "Iniciar novena",
        "novena.stop": "Detener novena",
        "novena.completeDay": "Completar día",
        "novena.completed": "Novena completada",
        "novena.completedTitle": "Novena completada",
        "novena.completedMessagePrefix": "Gracias por completar la",
        "novena.completedMessageSuffix": "novena.",
        "me.noneInProgress": "No hay novenas en curso.",
        "me.noneFavoriteNovenas": "Aún no hay novenas favoritas.",
        "me.noneFavoriteSaints": "Aún no hay santos favoritos.",
        "me.subtitle": "Tus novenas en curso y favoritos guardados.",
        "me.signedIn": "Sesión iniciada",
        "me.identitySupport": "Aquí viven tus favoritos, novenas activas y la futura configuración de tu cuenta.",
        "me.logout": "Cerrar sesión",
        "me.fallbackName": "Miembro de Sanctuary",
        "me.inProgress": "Novenas en curso",
        "me.favoriteNovenas": "Novenas favoritas",
        "me.favoriteSaints": "Santos favoritos",
        "me.reminders": "Recordatorios",
        "me.reminders.inProgressTitle": "Novenas en curso",
        "me.reminders.inProgressBody": "Envía recordatorios por la mañana y por la noche cuando tengas una novena en curso.",
        "me.reminders.generalTitle": "Recordatorio diario de Sanctuary",
        "me.reminders.generalBody": "Envía un recordatorio suave por la mañana cuando no tengas una novena en curso."
    ]

    private static let polish: [String: String] = [
        "tab.home": "Strona główna",
        "tab.novenas": "Nowenny",
        "tab.liturgical": "Liturgiczny",
        "tab.saints": "Święci",
        "tab.me": "Ja",
        "common.on": "Wł.",
        "common.off": "Wył.",
        "home.about": "O Sanctuary",
        "home.language": "Język",
        "home.welcome": "Witamy w twoim sanktuarium",
        "home.connect": "Jak chcesz połączyć się z Bogiem?",
        "home.supporting": "Modlitwa, liturgia i święci w jednym spokojnym miejscu.",
        "home.eyebrow": "#1 Katolicki towarzysz modlitwy",
        "home.saints": "Święci",
        "home.saintsSubtitle": "Święta i biografie",
        "home.novenasSubtitle": "Drogi modlitwy",
        "home.prayers": "Modlitwy",
        "home.prayersSubtitle": "Codzienne podstawy",
        "home.daily": "Czytania dzienne",
        "home.dailySubtitle": "Czytania i okresy",
        "home.intentions": "Intencje",
        "home.intentionsSubtitle": "Szukaj według potrzeby",
        "home.parish": "Znajdź moją parafię",
        "parish.title": "Znajdź moją parafię",
        "parish.subtitle": "Znajdź najbliższą parafię katolicką w pobliżu Twojej lokalizacji.",
        "parish.findButton": "Znajdź najbliższą parafię katolicką",
        "parish.distance": "Odległość",
        "parish.openMaps": "Otwórz w Mapach",
        "parish.website": "Strona parafii",
        "parish.error.locationDenied": "Aby znaleźć pobliskie parafie katolickie, wymagany jest dostęp do lokalizacji. Włącz go w Ustawieniach.",
        "parish.error.noLocation": "Nie udało się ustalić Twojej lokalizacji.",
        "parish.searching": "Szukamy najbliższej parafii...",
        "parish.searchingDetail": "Prosimy o cierpliwość. To może potrwać około minuty, gdy sprawdzamy pobliskie parafie katolickie.",
        "parish.error.noneFound": "Nie znaleziono pobliskiej parafii katolickiej.",
        "parish.error.generic": "Nie można teraz znaleźć pobliskiej parafii.",
        "common.loading": "Ładowanie Sanctuary...",
        "common.loadingDetail": "Poczekaj chwilę, aż treść się pojawi.",
        "home.chooseLanguage": "Wybierz język",
        "common.close": "Zamknij",
        "about.title": "Sanctuary",
        "about.heroTitle": "O Sanctuary",
        "about.subtitle": "Sanctuary to katolicki towarzysz modlitwy, czytań dnia, świętych, życia liturgicznego i nowenn.",
        "about.desktopVersion": "Korzystaj z Sanctuary na komputerze",
        "about.desktopBody": "Możesz także korzystać z Sanctuary na komputerze, aby uzyskać pełne doświadczenie webowe: czytania dnia, świętych, przegląd liturgiczny, nowenny i zsynchronizowane konto.",
        "about.versionTitle": "Wersja aplikacji",
        "about.versionLabel": "Wersja",
        "about.buildLabel": "Kompilacja",
        "about.environmentLabel": "Środowisko",
        "about.whatsInApp": "Co zawiera Sanctuary",
        "about.references": "Źródła",
        "about.contact": "Kontakt i opinie",
        "about.item.liturgical": "• Liturgia: widoki dnia, tygodnia i miesiąca z kontekstem okresu oraz szybkim przejściem do czytań dnia.",
        "about.item.saints": "• Święci: zestawienia według daty, szczegółowe profile i wyszukiwanie treści.",
        "about.item.novenas": "• Nowenny: daty rozpoczęcia oparte na regułach, data zakończenia, wyszukiwanie intencji i śledzenie postępu.",
        "about.refsIntro": "Sanctuary korzysta obecnie z tych publicznych źródeł dla czytań i informacji o świętych.",
        "about.source.usccb": "• USCCB (czytania dnia)",
        "about.source.wikipedia": "• Wikipedia",
        "about.link.usccb": "Codzienne czytania USCCB",
        "about.link.wikipedia": "Wikipedia",
        "about.contactBody": "Aby zgłosić błąd, poprosić o poprawki lub wysłać ogólne uwagi, skontaktuj się bezpośrednio z zespołem aplikacji.",
        "about.contactBodyDetailed": "Aby zgłosić błąd, poprosić o poprawkę lub przesłać opinię, skontaktuj się z nami i podaj stronę lub funkcję, z której korzystałeś(-aś), wraz z krótkim opisem problemu.",
        "about.link.desktop": "Otwórz mydailysanctuary.com",
        "about.link.emailSupport": "Napisz do wsparcia",
        "about.link.support": "Wsparcie",
        "about.link.privacy": "Polityka prywatności",
        "about.link.reportBug": "Zgłoś błąd",
        "about.link.feedback": "Wyślij opinię",
        "about.copyright": "Sanctuary © 2026. Wszelkie prawa zastrzeżone.",
        "about.privacyEffectiveDate": "Data wejścia w życie: 13 kwietnia 2026",
        "about.support.helpTitle": "Pomoc i opinie",
        "about.support.helpBody": "Jeśli potrzebujesz pomocy z Sanctuary, chcesz zgłosić błąd lub zaproponować ulepszenie, skontaktuj się z nami pod adresem info@mydailysanctuary.com i podaj urządzenie, platformę oraz krótki opis problemu.",
        "about.support.featuresTitle": "Funkcje aplikacji",
        "about.support.featuresBody": "Sanctuary zawiera modlitwy katolickie, nowenny, rozważania o świętych, treści kalendarza liturgicznego oraz opcjonalne przypomnienia wspierające modlitwę przez cały dzień.",
        "about.support.responseTitle": "Czas odpowiedzi",
        "about.support.responseBody": "Dokładamy wszelkich starań, aby szybko odpowiadać na prośby o wsparcie.",
        "about.privacy.collectTitle": "Jakie informacje zbieramy",
        "about.privacy.collectBody": "Sanctuary został zaprojektowany tak, aby działać przede wszystkim z lokalną zawartością na Twoim urządzeniu. Nie wymagamy tworzenia konta, aby korzystać z aplikacji.",
        "about.privacy.locationTitle": "Dane lokalizacyjne",
        "about.privacy.locationBody": "Jeśli zdecydujesz się zezwolić na dostęp do lokalizacji, Sanctuary wykorzysta ją, aby pomóc znaleźć pobliskie parafie. Dostęp do lokalizacji jest opcjonalny i można go zmienić w dowolnym momencie w ustawieniach urządzenia.",
        "about.privacy.notificationsTitle": "Powiadomienia",
        "about.privacy.notificationsBody": "Jeśli zdecydujesz się zezwolić na powiadomienia, Sanctuary wykorzysta te uprawnienia do wysyłania przypomnień o modlitwie i aktywności nowenn. Powiadomienia są opcjonalne i można je wyłączyć w dowolnym momencie w ustawieniach urządzenia.",
        "about.privacy.sharingTitle": "Udostępnianie danych",
        "about.privacy.sharingBody": "Nie sprzedajemy Twoich danych osobowych.",
        "about.privacy.choicesTitle": "Twoje wybory",
        "about.privacy.choicesBody": "Możesz odmówić dostępu do lokalizacji i powiadomień, a mimo to nadal korzystać z podstawowej funkcjonalności aplikacji.",
        "about.privacy.contactTitle": "Kontakt",
        "about.privacy.contactBody": "Jeśli masz pytania dotyczące prywatności, skontaktuj się z info@mydailysanctuary.com.",
        "calendar.today": "Dzisiaj",
        "calendar.day": "Dzień",
        "calendar.week": "Tydzień",
        "calendar.month": "Miesiąc",
        "calendar.search": "Szukaj",
        "calendar.searchSaints": "Szukaj świętych",
        "calendar.searchNovenas": "Szukaj nowenn",
        "calendar.searchIntentions": "Szukaj intencji nowenny",
        "calendar.subtitle.novenas": "Nowenny • Dotknij, aby przejść",
        "calendar.subtitle.liturgical": "Liturgia • Dotknij, aby przejść",
        "calendar.subtitle.saints": "Święci • Dotknij, aby przejść",
        "calendar.noNovenaMapped": "Brak przypisanej nowenny dla",
        "calendar.noSaintMapped": "Brak przypisanego świętego dla",
        "calendar.noNovenaAvailable": "Brak dostępnej nowenny",
        "calendar.dailyReadings": "Czytania dnia",
        "calendar.noEntry": "Brak wpisu",
        "calendar.ok": "OK",
        "calendar.pickDate": "Wybierz datę",
        "calendar.openDailyReadings": "Otwórz czytania dnia",
        "calendar.openDetails": "Otwórz szczegóły",
        "season.advent": "Adwent",
        "season.christmas": "Boże Narodzenie",
        "season.lent": "Wielki Post",
        "season.easter": "Wielkanoc",
        "season.ordinary": "Okres zwykły",
        "weekday.sun": "Nd",
        "weekday.mon": "Pn",
        "weekday.tue": "Wt",
        "weekday.wed": "Śr",
        "weekday.thu": "Cz",
        "weekday.fri": "Pt",
        "weekday.sat": "Sb",
        "search.saintsTitle": "Szukaj świętych",
        "search.novenasTitle": "Szukaj nowenn",
        "search.prayersTitle": "Szukaj modlitw",
        "search.saintsPrompt": "Szukaj świętych",
        "search.novenasPrompt": "Szukaj nowenn",
        "search.intentionsPrompt": "Szukaj intencji (np. mamy, uzdrowienie, lęk...)",
        "search.novenaType": "Nowenna",
        "search.intentionsLabel": "Intencje",
        "search.prayersPrompt": "Szukaj modlitw...",
        "search.results": "wyników",
        "common.cancel": "Anuluj",
        "common.apply": "Zastosuj",
        "common.done": "Gotowe",
        "common.pickDateLabel": "Wybierz datę",
        "common.closeUpper": "Zamknij",
        "saints.feastShort": "Wspomnienie",
        "detail.feastDate": "Data wspomnienia",
        "detail.endDate": "Data zakończenia",
        "detail.novenaStartDate": "Data rozpoczęcia nowenny",
        "detail.novenaEndDate": "Data zakończenia nowenny",
        "detail.summary": "Podsumowanie",
        "detail.biography": "Biografia",
        "detail.patronages": "Patronaty",
        "detail.prayers": "Modlitwy",
        "detail.sources": "Źródła",
        "detail.relatedNovenas": "Powiązane nowenny",
        "detail.relatedSaints": "Powiązani święci",
        "detail.addFavorites": "Dodaj do ulubionych",
        "detail.savedFavorites": "Zapisano w ulubionych",
        "detail.note": "Notatka",
        "novena.chooseDay": "Wybierz dzień",
        "novena.dayLabel": "Dzień",
        "novena.noDayContent": "Brak treści dla tego dnia nowenny.",
        "novena.scripture": "Pismo",
        "novena.prayer": "Modlitwa",
        "novena.reflection": "Rozważanie",
        "novena.loginPrompt": "Zaloguj się lub zarejestruj, aby rozpocząć tę nowennę i śledzić postępy.",
        "novena.start": "Rozpocznij nowennę",
        "novena.stop": "Zatrzymaj nowennę",
        "novena.completeDay": "Ukończ dzień",
        "novena.completed": "Nowenna ukończona",
        "novena.completedTitle": "Nowenna ukończona",
        "novena.completedMessagePrefix": "Dziękujemy za ukończenie nowenny",
        "novena.completedMessageSuffix": ".",
        "me.noneInProgress": "Brak nowenn w trakcie.",
        "me.noneFavoriteNovenas": "Brak ulubionych nowenn.",
        "me.noneFavoriteSaints": "Brak ulubionych świętych.",
        "me.subtitle": "Twoje trwające nowenny i zapisane ulubione.",
        "me.signedIn": "Zalogowano",
        "me.identitySupport": "Tutaj znajdują się ulubione, aktywne nowenny i przyszłe ustawienia konta.",
        "me.logout": "Wyloguj",
        "me.fallbackName": "Członek Sanctuary",
        "me.inProgress": "Nowenny w trakcie",
        "me.favoriteNovenas": "Ulubione nowenny",
        "me.favoriteSaints": "Ulubieni święci",
        "me.reminders": "Przypomnienia",
        "me.reminders.inProgressTitle": "Nowenny w toku",
        "me.reminders.inProgressBody": "Wysyłaj poranne i wieczorne przypomnienia, gdy masz nowennę w toku.",
        "me.reminders.generalTitle": "Codzienne przypomnienie Sanctuary",
        "me.reminders.generalBody": "Wysyłaj delikatne poranne przypomnienie, gdy nie masz nowenny w toku."
    ]
}
