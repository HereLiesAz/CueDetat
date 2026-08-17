# --- FILE: feature_expert_ar/proguard-rules.pro ---

# R8 configuration for the :feature_expert_ar dynamic feature module.
# Merged into the app-level release R8 pass alongside app/proguard-rules.pro
# (dynamic-feature modules are shrunk together with the base app; see AGP docs
# on per-module proguardFiles for dynamic-feature modules).

#-------------------------------------------------------------------------------
# ArControllerImpl: loaded reflectively from the base module
#-------------------------------------------------------------------------------
# ArControllerFacade (in :app) resolves this class purely via
# Class.forName("com.hereliesaz.cuedetat.feature.expert.ar.ArControllerImpl", ...)
# .getConstructor(Context::class.java) after the split installs. Nothing in
# this module references the class at compile time either (it is instantiated
# only through the base's reflective facade), so without this rule R8 would
# strip or rename it, or its constructor and members called reflectively.
-keep class com.hereliesaz.cuedetat.feature.expert.ar.ArControllerImpl {
    public <init>(android.content.Context);
    *;
}
