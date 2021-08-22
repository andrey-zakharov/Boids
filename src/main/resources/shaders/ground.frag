#version 130
#ifdef GL_ES
    precision mediump float;
#endif
uniform float div;
uniform int nbyte;
uniform isampler2D u_texture;
uniform mat4 u_projTrans;

in vec4 v_color;
in vec2 v_texCoords;

out vec4 out_color;
const vec4 defaultColor = vec4(1., 0xC5 / 255., 0.,  1.);

void main() {
    //ivec2 s = textureSize(u_texture);
    int ground = texture(u_texture, v_texCoords).r;

    float a = float(ground);
    vec3 color;
    switch(uint(ground)) {
        case 1u:
            color = vec3( 0.0, 0.0, 1.0);
            break;
        case 2u:
            color = vec3( 0.0, 1.0, 0.0);
            break;
        case 3u:
            color = vec3(0x8b / 255., 0x45 / 255., 0x13 / 255.);
            break;
        default:
            out_color = defaultColor;
            out_color.a = div / 100.;
            return;
    }

     out_color  = vec4(color, 1.0);
}