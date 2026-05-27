package com.sonar.plugins.mulesoft.profile;

import com.sonar.plugins.mulesoft.language.MuleLanguage;
import org.junit.jupiter.api.Test;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition.Context;
import static org.assertj.core.api.Assertions.assertThat;

class MuleQualityProfilesTest {

    @Test
    void three_profiles_are_defined() {
        Context ctx = new BuiltInQualityProfilesDefinition.Context();
        new MuleSanityProfile().define(ctx);
        new MuleRecommendedProfile().define(ctx);
        new MuleStrictProfile().define(ctx);

        assertThat(ctx.profile(MuleLanguage.LANGUAGE_KEY, "Mule Sanity")).isNotNull();
        assertThat(ctx.profile(MuleLanguage.LANGUAGE_KEY, "Mule Recommended")).isNotNull();
        assertThat(ctx.profile(MuleLanguage.LANGUAGE_KEY, "Mule Strict")).isNotNull();
    }

    @Test
    void recommended_is_the_default_profile() {
        Context ctx = new BuiltInQualityProfilesDefinition.Context();
        new MuleRecommendedProfile().define(ctx);
        assertThat(ctx.profile(MuleLanguage.LANGUAGE_KEY, "Mule Recommended").isDefault()).isTrue();
    }
}
