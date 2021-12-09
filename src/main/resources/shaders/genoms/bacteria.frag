#version 130
#ifdef GL_ES
precision mediump float;
#endif
uniform float time;
uniform int max_index;
uniform int max_age;
uniform isampler2D u_texture;
//uniform sampler2D u_texture;
uniform ivec2 u_resolution;
uniform sampler2D u_age;
uniform sampler2D u_energy;
uniform mat4 u_projTrans;
uniform ivec2 u_selected; // not normalized
uniform vec2 u_hovered;

in vec4 v_color;
in vec2 v_texCoords;
out vec4 out_color;

void main() {
    //ivec2 s = textureSize(u_texture);
    int index = texture(u_texture, v_texCoords).r - 1;
    if ( index < 0 ) {
        out_color = vec4(0.1, 0.1, 0.1, 0.1);
        return;
    }
    ivec2 tex_size = textureSize(u_texture, 0);
    float age = texelFetch(u_age, ivec2(index % tex_size.x, index / tex_size.x), 0).r;
    float energy = texelFetch(u_energy, ivec2(index % tex_size.x, index / tex_size.x), 0).r;
    float alpha = 0.7;
    ivec2 cd = ivec2(v_texCoords.x * tex_size.x, v_texCoords.y * tex_size.y);
    float d = length(vec2(cd - u_selected));
    if (0 <= d && d < 1) {
        out_color = vec4(0.5, 1.0, 0.5, 0.95);
        return;
    }
    //vec4 p = texture(u_energy, v_texCoords);
    out_color = vec4(age, age, energy, alpha);
    //out_color = vec4(texture2D(u_age, ivec2(index, 0), 0).rgb, 1.0);
}