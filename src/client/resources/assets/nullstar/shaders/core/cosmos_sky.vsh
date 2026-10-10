#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:fog.glsl>
#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>

layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 UV0;
layout(location = 2) in vec4 Color;
layout(location = 3) in vec3 Normal;

layout(location = 0) out vec4 vertexColor;
layout(location = 1) out vec2 params;
layout(location = 2) out vec3 skyDirection;

// The dome is emitted around the virtual camera. Normal carries the unrotated virtual-space direction so the
// procedural sky turns with the authored camera, not with the player's real view.
void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexColor = Color;
    params = UV0;
    skyDirection = Normal;
}
