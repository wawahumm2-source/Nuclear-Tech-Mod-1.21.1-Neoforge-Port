package com.hbm.client.weapon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HitFeedbackAnimationTest {
    @Test
    void strongerFeedbackSupersedesAWeakerMarkerAndFadesSmoothly() {
        HitFeedbackAnimation animation = new HitFeedbackAnimation();
        animation.start(HitFeedbackAnimation.Kind.HIT);
        HitFeedbackAnimation.Frame first = animation.sample(0.0F);
        animation.start(HitFeedbackAnimation.Kind.HEADSHOT);
        HitFeedbackAnimation.Frame headshot = animation.sample(0.0F);

        assertEquals(HitFeedbackAnimation.Kind.HIT, first.kind());
        assertEquals(HitFeedbackAnimation.Kind.HEADSHOT, headshot.kind());
        assertTrue(headshot.armLength() > first.armLength());

        animation.tick();
        HitFeedbackAnimation.Frame later = animation.sample(0.5F);
        assertTrue(later.alpha() < headshot.alpha());
        assertTrue(later.expansion() > headshot.expansion());
    }

    @Test
    void weakerLatePacketDoesNotReplaceKillFeedback() {
        HitFeedbackAnimation animation = new HitFeedbackAnimation();
        animation.start(HitFeedbackAnimation.Kind.KILL);
        animation.start(HitFeedbackAnimation.Kind.HIT);
        assertEquals(HitFeedbackAnimation.Kind.KILL, animation.sample(0.0F).kind());
    }

    @Test
    void headshotKillHasHighestPriority() {
        HitFeedbackAnimation animation = new HitFeedbackAnimation();
        animation.start(HitFeedbackAnimation.Kind.HEADSHOT_KILL);
        animation.start(HitFeedbackAnimation.Kind.KILL);
        assertEquals(HitFeedbackAnimation.Kind.HEADSHOT_KILL,
                animation.sample(0.0F).kind());
    }
}
