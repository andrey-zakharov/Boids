#version 130
#ifdef GL_ES
    precision mediump float;
#endif
uniform float time;
uniform float max_light;
uniform int nbyte;
uniform sampler2D u_texture;
uniform sampler2D u_obstacle;
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

    ivec2 texSize = textureSize(u_texture, 0);
    ivec2 denormalizedTexCoords = ivec2(v_texCoords.x * texSize.x, v_texCoords.y * texSize.y);

    float r = 0.;
    float b = 0.;

    if ( cell == 1 /*obstacle*/ ) {
        vec2 withinCell = vec2(v_texCoords - (denormalizedTexCoords / vec2(texSize))) * vec2(texSize);
        out_color = vec4(texture(u_obstacle, withinCell).rgb, 1.0);
        return;
    }

    if ( cell == 4 /* food */) {
        b = 0.5;
    }
    //out_color  = vec4(ground / max_light, 0.0, 0., 1.0);

    out_color = vec4(r, ground , b, 0.2);
}