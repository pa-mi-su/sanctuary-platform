import Foundation
import UserNotifications

@main
struct ReminderClockChecks {
    static func main() throws {
        for zone in ["America/New_York", "Europe/Warsaw", "Asia/Kolkata"] {
            NSTimeZone.default = TimeZone(identifier: zone)!
            for (hour, minute) in [(14, 55), (15, 0), (0, 0), (23, 59)] {
                let record = AccountContentReminder(kind: "prayers", slug: "clock-check", title: "Clock check", hour: hour, minute: minute)
                let decoded = try JSONDecoder().decode(AccountContentReminder.self, from: JSONEncoder().encode(record))
                precondition(decoded == record, "API round-trip changed reminder time")
                var pickerClock = Calendar(identifier: .gregorian); pickerClock.timeZone = TimeZone(secondsFromGMT: 0)!
                let displayed = pickerClock.dateComponents([.hour, .minute], from: decoded.time)
                precondition(displayed.hour == hour && displayed.minute == minute, "Time picker changed selected minutes")
                var components = DateComponents(); components.timeZone = TimeZone(identifier: zone); components.hour = hour; components.minute = minute
                let next = UNCalendarNotificationTrigger(dateMatching: components, repeats: true).nextTriggerDate()!
                var nativeClock = Calendar(identifier: .gregorian); nativeClock.timeZone = components.timeZone!
                let native = nativeClock.dateComponents([.hour, .minute], from: next)
                precondition(native.hour == hour && native.minute == minute, "Native trigger changed selected time")
            }
        }
        var calendar = Calendar(identifier: .gregorian); calendar.timeZone = TimeZone(identifier: "America/New_York")!
        let year = calendar.component(.year, from: Date()) + 1
        let marchFirst = calendar.date(from: DateComponents(year: year, month: 3, day: 1))!
        let firstSunday = 1 + (8 - calendar.component(.weekday, from: marchFirst)) % 7
        var spring = DateComponents(); spring.timeZone = calendar.timeZone; spring.year = year; spring.month = 3; spring.day = firstSunday + 7; spring.hour = 2; spring.minute = 30
        let next = UNCalendarNotificationTrigger(dateMatching: spring, repeats: false).nextTriggerDate()!
        let shifted = calendar.dateComponents([.hour, .minute], from: next)
        precondition(shifted.hour == 3 && shifted.minute == 30, "Update Android DST policy if Apple's native behavior changes")
        print("Reminder clock checks passed: API round trips, 12 time/zone cases, native minute preservation and DST gap behavior.")
    }
}
