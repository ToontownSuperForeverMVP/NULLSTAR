#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:fog.glsl>
#include <minecraft:dynamictransforms.glsl>
#include <minecraft:globals.glsl>

layout(location = 0) in vec4 vertexColor;
layout(location = 1) in vec2 params;
layout(location = 2) in vec3 skyDirection;

layout(location = 0) out vec4 fragColor;

// NULLSTAR procedural cosmos. One opaque dome replaces everything behind the cosmic act.
//   params.x  journey coordinate: 0 atmosphere, 1 interstellar (Milky Way), 2 intergalactic, 3 cosmic web, 4 beyond
//   params.y  scene time in seconds
//   color.r   exposure, color.g discharge flash, color.b nebula strength, color.a galactic band strength
// No fog is applied: the dome is the far field of an authored virtual scene, not part of the Minecraft world.

uint hash3u(uvec3 v) {
    v = v * 1664525u + 1013904223u;
    v.x += v.y * v.z; v.y += v.z * v.x; v.z += v.x * v.y;
    v ^= v >> 16u;
    v.x += v.y * v.z; v.y += v.z * v.x; v.z += v.x * v.y;
    return v.x ^ v.y ^ v.z;
}
vec3 hash33(vec3 p) {
    uvec3 q = uvec3(ivec3(floor(p)) + ivec3(4096));
    uint h = hash3u(q);
    return vec3(float(h & 1023u), float((h >> 10u) & 1023u), float((h >> 20u) & 1023u)) / 1023.0;
}
float hash13(vec3 p) { return hash33(p).x; }

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

vec3 starTint(float t) {
    // Blackbody-like ramp from red dwarfs to blue giants.
    vec3 red = vec3(1.0, 0.62, 0.42), white = vec3(1.0, 0.96, 0.9), blue = vec3(0.68, 0.8, 1.0);
    return t < 0.5 ? mix(red, white, t * 2.0) : mix(white, blue, (t - 0.5) * 2.0);
}

// One layer of point stars: at most one star per 3D cell, projected onto the sphere so it never smears.
vec3 starLayer(vec3 d, float density, float probability, float gain, float twinkle) {
    vec3 p = d * density;
    vec3 cell = floor(p);
    vec3 h = hash33(cell + 0.5);
    if (h.x > probability) return vec3(0.0);
    vec3 star = normalize(cell + 0.25 + 0.5 * hash33(cell + 19.7)) * density;
    float dist = length(p - star);
    float width = max(0.07, fwidth(p.x) * 0.9 + fwidth(p.y) * 0.9);
    float core = exp(-dist * dist / (width * width));
    float brightness = pow(h.y, 9.0) * 2.8 + 0.06;
    float tw = 1.0 + twinkle * sin(params.y * (1.3 + h.z * 3.1) + h.y * 40.0) * 0.35;
    return starTint(h.z) * core * brightness * gain * tw;
}

// Milky Way: a dusty band around a tilted great circle with a warm bulge toward the galactic centre.
vec3 galacticBand(vec3 d) {
    vec3 normal = normalize(vec3(0.32, 0.86, -0.4));
    vec3 centre = normalize(vec3(0.82, -0.12, 0.56));
    float lat = dot(d, normal);
    float along = dot(d, centre);
    float band = exp(-lat * lat / 0.018);
    float clump = fbm(d * 7.0, 5);
    float dust = smoothstep(0.42, 0.72, fbm(d * 11.0 + vec3(3.0), 5)) * exp(-lat * lat / 0.004);
    float bulge = exp(-pow(1.0 - max(along, 0.0), 2.0) * 9.0) * exp(-lat * lat / 0.05);
    float glow = band * (0.35 + 0.9 * clump) * (1.0 - 0.85 * dust) + bulge * (0.3 + 0.3 * clump) * (1.0 - 0.6 * dust);
    vec3 cool = vec3(0.55, 0.62, 0.85), warm = vec3(1.0, 0.82, 0.58);
    return mix(cool, warm, clamp(bulge * 1.6 + along * 0.2, 0.0, 1.0)) * glow * 0.42;
}

// Emission nebula with domain warping; palette chosen by the journey stage.
vec3 nebula(vec3 d, vec3 a, vec3 b, vec3 c, float scale, float seed) {
    vec3 q = d * scale + seed;
    vec3 warp = vec3(fbm(q, 4), fbm(q + 5.2, 4), fbm(q + 9.7, 4));
    float n = fbm(q + warp * 2.2 + vec3(0.0, params.y * 0.004, 0.0), 5);
    float density = smoothstep(0.38, 0.85, n);
    float filament = pow(1.0 - abs(fbm(q * 2.1 + warp, 4) * 2.0 - 1.0), 6.0);
    vec3 col = mix(a, b, smoothstep(0.3, 0.8, warp.x));
    col = mix(col, c, filament * 0.7);
    return col * (density * 0.7 + filament * density * 0.9);
}

// Cosmic web: Voronoi cell walls become filaments; walls' junctions become glowing galaxy clusters.
vec3 cosmicWeb(vec3 d, float time) {
    vec3 p = d * 4.2 + vec3(0.0, 0.0, time * 0.01);
    // Domain warp bends the cell walls into organic filaments instead of a cracked-glass lattice.
    p += vec3(fbm(d * 3.1, 3), fbm(d * 3.1 + 7.7, 3), fbm(d * 3.1 + 3.3, 3)) * 1.1 - 0.55;
    vec3 cell = floor(p), f = fract(p);
    float f1 = 8.0, f2 = 8.0;
    for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) {
        vec3 o = vec3(x, y, z);
        vec3 r = o + hash33(cell + o) - f;
        float dist = dot(r, r);
        if (dist < f1) { f2 = f1; f1 = dist; } else if (dist < f2) f2 = dist;
    }
    float wall = sqrt(f2) - sqrt(f1);
    float filament = exp(-wall * wall / 0.0016) * smoothstep(0.25, 0.7, fbm(p * 1.7, 3));
    float clusters = pow(filament, 3.0) * smoothstep(0.55, 0.95, noise3(p * 3.0));
    float detail = 0.55 + 0.6 * fbm(d * 22.0, 4);
    vec3 cold = vec3(0.32, 0.42, 0.95), hot = vec3(1.0, 0.72, 0.45);
    return mix(cold, hot, clusters) * (filament * 0.2 * detail + clusters * 1.4);
}

void main() {
    vec3 d = normalize(skyDirection);
    float s = params.x;
    float exposure = vertexColor.r * 1.6;
    float flash = vertexColor.g;
    float nebulaGain = vertexColor.b * 1.5;
    float bandGain = vertexColor.a * 1.5;

    float wAtmos = clamp(1.0 - s, 0.0, 1.0);
    float wStars = clamp(1.0 - abs(s - 1.0), 0.0, 1.0);
    float wVoid = clamp(1.0 - abs(s - 2.0), 0.0, 1.0);
    float wWeb = clamp(1.0 - abs(s - 3.0), 0.0, 1.0);
    float wBeyond = clamp(s - 3.0, 0.0, 1.0);

    vec3 col = vec3(0.004, 0.005, 0.012);

    // Stars dominate the near journey, thin out between galaxies and return sparsely beyond the boundary.
    float starGain = max(wStars, (1.0 - wAtmos) * step(s, 1.0)) + 0.18 * wVoid + 0.45 * wBeyond;
    if (starGain > 0.001) {
        vec3 stars = starLayer(d, 60.0, 0.5, 1.5, 1.0) + starLayer(d, 150.0, 0.28, 0.5, 0.6) + starLayer(d, 360.0, 0.2, 0.22, 0.0);
        col += stars * starGain;
    }
    if (wStars > 0.001 || (wAtmos < 1.0 && s <= 1.0)) {
        float g = max(wStars, (1.0 - wAtmos) * step(s, 1.0));
        col += galacticBand(d) * g * bandGain;
        col += nebula(d, vec3(0.32, 0.12, 0.42), vec3(0.08, 0.3, 0.36), vec3(0.9, 0.4, 0.5), 2.2, 3.0) * g * 0.22 * nebulaGain;
    }
    if (wVoid > 0.001) {
        // Intergalactic dark: distant galaxies as tiny elongated smudges.
        vec3 p = d * 46.0;
        vec3 cell = floor(p);
        vec3 h = hash33(cell + 3.3);
        if (h.x < 0.22) {
            vec3 c = normalize(cell + 0.5) * 46.0;
            vec3 o = p - c;
            float e = length(o * vec3(1.0, 2.6 + h.y * 3.0, 1.6));
            col += mix(vec3(0.8, 0.75, 1.0), vec3(1.0, 0.85, 0.6), h.z) * exp(-e * e * 9.0) * 0.6 * wVoid;
        }
        col += nebula(d, vec3(0.05, 0.06, 0.14), vec3(0.12, 0.06, 0.16), vec3(0.3, 0.3, 0.6), 1.4, 11.0) * 0.18 * wVoid;
    }
    if (wWeb > 0.001) col += cosmicWeb(d, params.y) * wWeb * 1.2;
    if (wBeyond > 0.001) {
        // Beyond the boundary: the stellar foundry where the mothership harvests stars. Rich, hot and alien.
        vec3 neb = nebula(d, vec3(0.55, 0.16, 0.06), vec3(0.08, 0.28, 0.4), vec3(1.0, 0.72, 0.36), 1.7, 41.0);
        neb += nebula(d.zxy, vec3(0.22, 0.05, 0.26), vec3(0.6, 0.3, 0.1), vec3(1.0, 0.5, 0.35), 3.1, 77.0) * 0.5;
        col += neb * wBeyond * nebulaGain * 0.9;
        col += cosmicWeb(d, params.y) * wBeyond * 0.25;
    }
    if (wAtmos > 0.001) {
        // Daylit sky thinning to black: horizon haze, deep zenith blue, a warm limb toward the sun.
        float up = d.y;
        vec3 horizon = vec3(0.62, 0.78, 0.98), zenith = vec3(0.12, 0.28, 0.62), space = vec3(0.01, 0.015, 0.04);
        float altitude = 1.0 - wAtmos;
        vec3 sky = mix(horizon, zenith, smoothstep(-0.05, 0.6, up));
        sky = mix(sky, space, smoothstep(0.0, 1.0, altitude * 1.25 + max(up, 0.0) * altitude));
        float sun = pow(max(dot(d, normalize(vec3(-0.5, 0.25, -0.8))), 0.0), 6.0);
        sky += vec3(1.0, 0.75, 0.45) * sun * 0.25 * wAtmos;
        col = mix(col, sky, wAtmos);
    }

    col *= exposure;
    col += vec3(1.0, 0.86, 0.62) * flash;
    // Gentle filmic shoulder keeps bright filaments from clipping into flat white.
    col = col / (1.0 + col * 0.35);
    fragColor = vec4(col, 1.0);
}
