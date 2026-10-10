#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:fog.glsl>
#include <minecraft:dynamictransforms.glsl>
#include <minecraft:globals.glsl>
#include <minecraft:oit.glsl>

layout(location = 0) in float sphericalVertexDistance;
layout(location = 1) in float cylindricalVertexDistance;
layout(location = 2) in vec4 vertexColor;
layout(location = 3) in vec2 texCoord0;
layout(location = 4) in vec3 surfaceNormal;
layout(location = 5) in vec3 viewVector;

#ifndef OIT_ALPHA_ONLY
layout(location = 0) out vec4 fragColor;
#endif

// NULLSTAR procedural energy materials (additive). The material mode and a 0..1 control value travel in the
// texture coordinates so one compiled program serves every cosmic and colossal surface:
//   v = local v + 128 * mode             (local v in [-64, 64))
//   u = local u + 4 * k                  (local u in [-2, 2), control = k / 32)
// Modes: 0 galaxy disc, 1 star surface, 2 corona, 3 beam, 4 nebula, 5 atmosphere, 6 shock front, 7 flare.

uint hash3u(uvec3 v) {
    v = v * 1664525u + 1013904223u;
    v.x += v.y * v.z; v.y += v.z * v.x; v.z += v.x * v.y;
    v ^= v >> 16u;
    v.x += v.y * v.z; v.y += v.z * v.x; v.z += v.x * v.y;
    return v.x ^ v.y ^ v.z;
}
float hash13(vec3 p) {
    uvec3 q = uvec3(ivec3(floor(p)) + ivec3(4096));
    return float(hash3u(q) & 65535u) / 65535.0;
}
float noise3(vec3 p) {
    vec3 i = floor(p), f = fract(p);
    vec3 u = f * f * (3.0 - 2.0 * f);
    float n000 = hash13(i), n100 = hash13(i + vec3(1, 0, 0)), n010 = hash13(i + vec3(0, 1, 0)), n110 = hash13(i + vec3(1, 1, 0));
    float n001 = hash13(i + vec3(0, 0, 1)), n101 = hash13(i + vec3(1, 0, 1)), n011 = hash13(i + vec3(0, 1, 1)), n111 = hash13(i + vec3(1, 1, 1));
    return mix(mix(mix(n000, n100, u.x), mix(n010, n110, u.x), u.y), mix(mix(n001, n101, u.x), mix(n011, n111, u.x), u.y), u.z);
}
float fbm(vec3 p, int octaves) {
    float sum = 0.0, amp = 0.5;
    for (int i = 0; i < octaves; i++) { sum += amp * noise3(p); p = p * 2.03 + vec3(17.1, 3.7, 9.2); amp *= 0.5; }
    return sum;
}
float ridge(vec3 p, int octaves) {
    float sum = 0.0, amp = 0.5;
    for (int i = 0; i < octaves; i++) { sum += amp * (1.0 - abs(noise3(p) * 2.0 - 1.0)); p = p * 2.1 + vec3(5.3, 1.1, 7.7); amp *= 0.5; }
    return sum;
}

const float TAU = 6.2831853;

vec4 calculateFinalColor(vec4 color) {
    #ifdef OIT_ACCUMULATE
    color = sampleColorForAccumulation(color);
    #endif
    return color;
}

void main() {
    float t = GameTime * 1200.0;
    int mode = int(floor((texCoord0.y + 64.0) / 128.0));
    float v = texCoord0.y - 128.0 * float(mode);
    float k = floor((texCoord0.x + 2.0) / 4.0);
    float u = texCoord0.x - 4.0 * k;
    float control = clamp(k / 32.0, 0.0, 1.0);
    vec3 tint = vertexColor.rgb;
    float strength = vertexColor.a;
    vec3 n = normalize(surfaceNormal + vec3(0.0, 1.0e-5, 0.0));
    vec3 view = normalize(viewVector + vec3(1.0e-5));
    float facingSigned = dot(n, view);
    float facing = abs(facingSigned);
    vec3 rgb = tint;
    float intensity = 0.0;

    if (mode == 0) {
        // Barred spiral galaxy: logarithmic arms, dust lanes, star-forming knots and a hot bulge.
        vec2 p = vec2(u, v);
        float r = length(p);
        float th = atan(p.y, p.x + 1.0e-6);
        float spiral = th - log(r + 0.04) * 2.4;
        float arms = pow(max(0.0, 0.5 + 0.5 * cos(2.0 * spiral)), 2.4);
        float minor = pow(max(0.0, 0.5 + 0.5 * cos(4.0 * spiral + 1.3)), 5.0) * 0.45;
        float clumps = fbm(vec3(p * 9.0, 1.7), 5);
        float lane = pow(max(0.0, 0.5 + 0.5 * cos(2.0 * (spiral - 0.42))), 10.0) * smoothstep(0.08, 0.3, r);
        float disc = exp(-r * 3.4);
        float bar = exp(-pow(abs(dot(p, vec2(0.86, 0.5))) / 0.24, 2.0) - pow(abs(dot(p, vec2(-0.5, 0.86))) / 0.065, 2.0));
        float bulge = exp(-r * r / 0.012);
        float speck = step(0.985, hash13(vec3(floor(p * 260.0), 3.0))) * (arms + minor) * (1.0 - r);
        float knots = smoothstep(0.62, 0.8, clumps) * arms * smoothstep(0.15, 0.4, r);
        float body = disc * (0.25 + (arms + minor) * (0.55 + 0.9 * clumps)) * (1.0 - 0.8 * lane * clumps);
        vec3 armColor = mix(vec3(0.55, 0.68, 1.0), tint, 0.4);
        rgb = armColor * body + vec3(1.0, 0.86, 0.62) * (bulge * 1.3 + bar * 0.6) + vec3(1.0, 0.45, 0.65) * knots * 0.9
                + vec3(0.9, 0.95, 1.0) * speck * 1.6;
        rgb *= smoothstep(1.0, 0.72, r);
        intensity = strength;
    } else if (mode == 1) {
        // Contained star photosphere: seamless 3D granulation from the sphere's own parameterisation.
        if (facingSigned < 0.0) discard;
        float az = u * TAU, polar = v * 3.14159265;
        vec3 dir = vec3(sin(polar) * cos(az), cos(polar), sin(polar) * sin(az));
        float instability = control;
        vec3 flow = vec3(t * 0.05, -t * 0.08, t * 0.03);
        vec3 warp = vec3(fbm(dir * 3.0 + flow, 3), fbm(dir * 3.0 + flow + 7.3, 3), 0.0);
        float granules = ridge(dir * 11.0 + warp * 2.5 - flow * 2.0, 4);
        float storms = smoothstep(0.55, 0.9, fbm(dir * 2.4 + flow * 1.6 + 3.1, 4));
        float spots = smoothstep(0.7, 0.85, fbm(dir * 4.0 - flow, 3)) * (1.0 - instability);
        float limb = clamp(facingSigned, 0.0, 1.0);
        float darkening = 0.38 + 0.62 * pow(limb, 0.45);
        float flicker = 1.0 + instability * 0.25 * sin(t * 9.0 + granules * 12.0);
        float heat = granules * 0.75 + storms * (0.4 + instability * 1.2);
        rgb = mix(tint * 0.85, vec3(1.0, 0.97, 0.9), clamp(heat * 0.55 + pow(limb, 3.0) * 0.35, 0.0, 1.0));
        rgb = mix(rgb, vec3(0.45, 0.12, 0.03), spots * 0.7);
        intensity = strength * darkening * (0.55 + 0.75 * heat) * flicker;
    } else if (mode == 2) {
        // Corona: radial streamers rotating slowly, brightening and lengthening with instability.
        vec2 p = vec2(u, v);
        float r = length(p);
        float a = atan(p.y, p.x + 1.0e-6);
        vec3 q = vec3(cos(a) * 3.5, sin(a) * 3.5, r * 1.5 - t * 0.18);
        float streamers = pow(fbm(q, 4), 2.0) * (1.4 + control * 1.8);
        float halo = exp(-r * (7.0 - control * 2.5));
        float rays = streamers * exp(-r * (3.2 - control * 1.2));
        rgb = mix(tint, vec3(1.0, 0.95, 0.85), halo * 0.6);
        intensity = strength * (halo * 0.9 + rays) * smoothstep(1.0, 0.55, r);
    } else if (mode == 3) {
        // Beam: incandescent core, helical containment bands, sliding plasma filaments, turbulent envelope.
        float rimness = control;
        float around = u * TAU;
        vec3 q = vec3(cos(around) * 1.6, sin(around) * 1.6, v * 5.0 - t * 9.0);
        float plasma = fbm(q, 4);
        float boil = ridge(q * 1.7 + vec3(0.0, 0.0, -t * 4.0), 3);
        float helix = pow(max(0.0, 0.5 + 0.5 * sin(around * 3.0 + v * 18.0 - t * 15.0)), 10.0);
        float filament = pow(max(0.0, noise3(vec3(cos(around) * 9.0, sin(around) * 9.0, v * 0.8 - t * 3.0)) - 0.35) / 0.65, 5.0);
        float core = pow(facing, 2.6);
        float envelope = pow(1.0 - facing, 1.8);
        float body = mix(core * (0.75 + 0.5 * plasma) + helix * facing * 0.45 + filament * 0.6,
                         envelope * (0.35 + 0.9 * boil) * (0.6 + 0.6 * plasma) + filament * 0.25, rimness);
        rgb = mix(tint, vec3(1.0, 0.98, 0.92), clamp(core * (1.0 - rimness) * 0.85 + helix * 0.3, 0.0, 1.0));
        intensity = strength * body;
    } else if (mode == 4) {
        // Nebula volume: domain-warped emission with bright ionisation fronts and a soft boundary.
        vec2 p = vec2(u, v);
        float r = length(p);
        vec3 q = vec3(p * 1.9, control * 13.0 + t * 0.01);
        vec3 warp = vec3(fbm(q, 4), fbm(q + 4.1, 4), 0.0) * 1.8;
        float density = smoothstep(0.32, 0.82, fbm(q + warp, 5));
        float front = pow(max(0.0, 1.0 - abs(fbm(q * 1.7 + warp, 4) * 2.0 - 1.0)), 7.0);
        rgb = mix(tint, vec3(1.0, 0.86, 0.68), front * 0.65);
        intensity = strength * (density * 0.75 + front * density * 1.2) * smoothstep(1.0, 0.25, r);
    } else if (mode == 5) {
        // Atmosphere: scattering concentrated at the limb; the vertex colour carries the baked day/night light.
        float f = 1.0 - facing;
        float scatter = pow(f, 3.5) * 1.6 + pow(f, 12.0) * 2.0;
        intensity = strength * (scatter + 0.04) * (facingSigned < 0.0 ? 0.6 : 1.0);
        rgb = mix(tint, vec3(0.9, 0.97, 1.0), pow(f, 6.0) * 0.4);
    } else if (mode == 6) {
        // Shock front: razor leading edge with turbulent trailing compression; u wraps the ring, v runs inner->outer.
        float around = u * TAU;
        float turb = fbm(vec3(cos(around) * 5.0, sin(around) * 5.0, v * 4.0 - t * 0.6 + control * 9.0), 4);
        float front = exp(-(1.0 - v) * 16.0);
        float trail = v * v * (0.25 + 0.9 * turb);
        float tear = smoothstep(0.35, 0.65, turb);
        rgb = mix(tint, vec3(1.0, 0.95, 0.85), front * 0.7);
        intensity = strength * (front * (0.8 + 0.6 * tear) + trail * 0.55) * smoothstep(0.0, 0.12, v);
    } else {
        // Flare: hot core, anamorphic streak and a faint starburst for discharge transients.
        vec2 p = vec2(u, v);
        float r = length(p);
        float a = atan(p.y, p.x);
        float core = exp(-r * 9.0);
        float streak = exp(-abs(p.y) * 34.0) * exp(-abs(p.x) * 2.0);
        float rays = pow(abs(cos(a * 3.0)), 60.0) * exp(-r * 4.0);
        rgb = mix(tint, vec3(1.0), core);
        intensity = strength * (core * 1.4 + streak * 0.7 + rays * 0.35) * smoothstep(1.0, 0.7, r);
    }

    // Any non-finite term would poison additive blending and paint black holes; drop such fragments instead.
    if (isnan(intensity) || isinf(intensity) || any(isnan(rgb)) || any(isinf(rgb))) discard;
    float fog = total_fog_value(sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd);
    float alpha = clamp(intensity, 0.0, 1.0) * (1.0 - 0.65 * fog);
    // Values above one keep their hue but saturate toward white so the brightest plasma reads as incandescent.
    rgb = mix(rgb, vec3(1.0, 0.98, 0.94), clamp(intensity - 1.0, 0.0, 0.7));
    vec4 color = vec4(rgb, alpha) * ColorModulator;
    #ifdef OIT_ALPHA_ONLY
    executeAlphaOnlyPhase(gl_FragCoord.z, color.a);
    #else
    fragColor = calculateFinalColor(color);
    #endif
}
