#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:fog.glsl>
#include <minecraft:dynamictransforms.glsl>
#include <minecraft:globals.glsl>
#include <minecraft:oit.glsl>

uniform sampler2D Sampler0;

layout(location = 0) in float sphericalVertexDistance;
layout(location = 1) in float cylindricalVertexDistance;
layout(location = 2) in vec4 vertexColor;
layout(location = 3) in vec2 texCoord0;
layout(location = 4) in float facing;

#ifndef OIT_ALPHA_ONLY
layout(location = 0) out vec4 fragColor;
#endif

vec4 calculateFinalColor(vec4 color) {
    #ifdef OIT_ACCUMULATE
    color = sampleColorForAccumulation(color);
    #endif
    return color;
}

void main() {
    // GameTime is the day-cycle fraction; 1200 converts it to seconds for a secondary flow layer.
    float seconds = GameTime * 1200.0;
    vec4 base = texture(Sampler0, texCoord0);
#ifdef MODE_FLAT
    float shape = base.a;
    float turbulence = 0.85 + 0.3 * base.r;
#else
    vec4 drift = texture(Sampler0, texCoord0 * vec2(1.73, 0.61) + vec2(seconds * 0.071, -seconds * 0.23));
    float turbulence = base.r * 0.55 + drift.g * 0.45 + drift.b * 0.6 + base.b * 0.25;
    #ifdef MODE_RIM
    float shape = pow(1.0 - facing, 2.0) * 1.35 + 0.08;
    #else
    float shape = pow(facing, 1.35);
    #endif
#endif
    float intensity = shape * turbulence;
    float fog = total_fog_value(sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd);
    // Hot regions bleach toward white, giving layered energy a bright nucleus without a bloom pass.
    vec3 rgb = mix(vertexColor.rgb, vec3(1.0, 0.98, 0.92), clamp((intensity * vertexColor.a - 0.75) * 0.9, 0.0, 0.8));
    float alpha = clamp(intensity * vertexColor.a, 0.0, 1.0) * (1.0 - 0.65 * fog);
    vec4 color = vec4(rgb, alpha) * ColorModulator;
    #ifdef OIT_ALPHA_ONLY
    executeAlphaOnlyPhase(gl_FragCoord.z, color.a);
    #else
    fragColor = calculateFinalColor(color);
    #endif
}
