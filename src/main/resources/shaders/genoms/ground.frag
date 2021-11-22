#version 130
#ifdef GL_ES
    precision mediump float;
#endif
uniform float time;
uniform float max_light;
uniform int nbyte;
uniform sampler2D u_texture;
uniform isampler2D u_cells;
uniform mat4 u_projTrans;

in vec4 v_color;
in vec2 v_texCoords;

out vec4 out_color;
const vec4 defaultColor = vec4(1., 0xC5 / 255., 0.,  1.);

void main() {
    //ivec2 s = textureSize(u_texture);
    float ground = texture(u_texture, v_texCoords).r;
    int cell = texture(u_cells, v_texCoords).r;
    float r = 0.;

    if ( cell == 1 /*obstacle*/ ) {
        r = 0.8;
    }
    //out_color  = vec4(ground / max_light, 0.0, 0., 1.0);

    out_color = vec4(cell, ground , 0., 0.2);
}