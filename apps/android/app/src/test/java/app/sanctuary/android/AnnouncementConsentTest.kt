package app.sanctuary.android
import org.junit.Assert.*
import org.junit.Test
class AnnouncementConsentTest {
 @Test fun missingChoiceInvitesWithoutSubscribing() { val state=AnnouncementConsent.restore(null,null);assertTrue(state.needsInvitation);assertFalse(state.enabled) }
 @Test fun updatesPreserveBothChoices() { for(choice in listOf(true,false)) { val state=AnnouncementConsent.restore(choice,null);assertEquals(choice,state.enabled);assertFalse(state.needsInvitation) } }
 @Test fun recoveryPreservesEnabledAndDisabled() { for(choice in listOf(true,false)) { val state=AnnouncementConsent.restore(null,choice);assertEquals(choice,state.enabled);assertFalse(state.needsInvitation) } }
 @Test fun explicitChoiceWinsOverOlderRecovery() { assertFalse(AnnouncementConsent.restore(false,true).enabled);assertTrue(AnnouncementConsent.restore(true,false).enabled) }
 @Test fun upgradePromptsOffOncePerVersionButNeverPromptsEnabledUsers() {
  assertTrue(AnnouncementConsent.restore(false,null,"1.0.37",null).needsInvitation)
  assertFalse(AnnouncementConsent.restore(false,null,"1.0.37","1.0.37").needsInvitation)
  assertTrue(AnnouncementConsent.restore(false,null,"1.0.38","1.0.37").needsInvitation)
  assertFalse(AnnouncementConsent.restore(true,null,"1.0.38",null).needsInvitation)
  assertTrue(AnnouncementConsent.restore(null,null,"1.0.37",null).needsInvitation)
 }
}
