#version 300 es

uniform mat4 u_ModelViewProjection;
uniform vec4 u_Color;

in vec3 a_Position;

out vec4 v_Color;

void main() {
    v_Color = u_Color;
    gl_Position = u_ModelViewProjection * vec4(a_Position, 1.0);
}