#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:fog.glsl>
#include <minecraft:dynamictransforms.glsl>
#include <minecraft:globals.glsl>

uniform sampler2D Sampler0;

layout(location = 0) in float sphericalVertexDistance;
layout(location = 1) in float cylindricalVertexDistance;
layout(location = 2) in vec4 vertexColor;
layout(location = 3) in vec2 texCoord0;
layout(location = 4) in vec3 worldNormal;
layout(location = 5) in vec3 viewVector;

layout(location = 0) out vec4 fragColor;

// Vertex RGB is a light tint where 0.625 is neutral. Vertex alpha is the seam energy level, or the
// heat level for DEBRIS. Texture alpha below one marks emissive pixels.
void main() {
    vec4 tex = texture(Sampler0, texCoord0);
#ifdef BAKED
    // Cosmic bodies: the scene bakes key, fill, point and rim light into vertex RGB (0.5 = unlit albedo) and the
    // emissive seam level into alpha. No world sun and no fog, because the virtual scene is not in the world.
    float seam = 1.0 - tex.a;
    float glowPulse = 0.9 + 0.1 * sin(GameTime * 1200.0 * 4.0 + texCoord0.y * 30.0);
    vec3 baked = tex.rgb * vertexColor.rgb * 2.0 + tex.rgb * seam * vertexColor.a * 4.5 * glowPulse;
    fragColor = vec4(baked, 1.0);
    return;
#endif
    vec3 n = normalize(worldNormal + vec3(0.0, 1.0e-4, 0.0));
    vec3 v = normalize(viewVector + vec3(1.0e-4));
    vec3 sun = normalize(vec3(0.38, 0.82, 0.42));
    float diffuse = max(dot(n, sun), 0.0);
    float sky = 0.5 + 0.5 * n.y;
    float rim = pow(1.0 - abs(dot(n, v)), 3.0);
    float spec = pow(max(dot(reflect(-sun, n), v), 0.0), 18.0) * 0.35;
    vec3 tint = vertexColor.rgb * 1.6;
    vec3 lit = tex.rgb * tint * (0.22 + 0.7 * diffuse + 0.22 * sky) + vec3(spec) * tint + rim * 0.12 * tint;
    float emissive = 1.0 - tex.a;
#ifdef DEBRIS
    float heat = vertexColor.a;
    lit = mix(lit, tex.rgb * vec3(1.6, 0.62, 0.18) + vec3(0.9, 0.32, 0.05), heat * heat);
#else
    float pulse = 0.85 + 0.15 * sin(GameTime * 1200.0 * 6.0 + texCoord0.x * 40.0);
    lit += tex.rgb * emissive * (0.35 + 3.2 * vertexColor.a) * pulse;
#endif
    vec4 color = vec4(lit, 1.0);
    fragColor = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
}
