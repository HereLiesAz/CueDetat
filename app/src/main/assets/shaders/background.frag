#version 300 es
#extension GL_OES_EGL_image_external_essl3 : require
// Simple pass-through fragment shader.
precision mediump float;

uniform samplerExternalOES s_Texture;
in vec2 v_TexCoord;
out vec4 outColor;

void main() {
    outColor = texture(s_Texture, v_TexCoord);
}