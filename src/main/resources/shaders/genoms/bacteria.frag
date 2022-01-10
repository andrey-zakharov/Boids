#version 130
#ifdef GL_ES
precision mediump float;
#endif
uniform float time;

uniform int w;
uniform int h;
uniform int max_index;
uniform int max_age;
uniform int gen_length;
uniform isampler2D u_texture;
//uniform sampler2D u_texture;
uniform ivec2 u_resolution;
uniform sampler2D u_age;
uniform sampler2D u_energy;
uniform isampler2D u_gen;
uniform mat4 u_projTrans;
uniform ivec2 u_selected; // not normalized
uniform vec2 u_hovered;
uniform int u_showLayer; // 0 -nothing 1 - age 2- energy 4- gen

in vec4 v_color;
in vec2 v_texCoords;
out vec4 out_color;

float rand(vec2 co){
    return fract(sin(dot(co, vec2(12.9898, 78.233))) * 43758.5453);
}

const vec4 bitEnc = vec4(1.,255.,65025.,16581375.);
const vec4 bitDec = 1./bitEnc;
vec4 EncodeFloatRGBA (float v) {
    vec4 enc = bitEnc * v;
    enc = fract(enc);
    enc -= enc.yzww * vec2(1./255., 0.).xxxy;
    return enc;
}
float DecodeFloatRGBA (vec4 v) {
    return dot(v, bitDec);
}

void main() {
    //ivec2 s = textureSize(u_texture);
    int index = texture(u_texture, v_texCoords).r ;
    ivec2 tex_size = textureSize(u_texture, 0);
    float age = texelFetch(u_age, ivec2(index % tex_size.x, index / tex_size.x), 0).r;
    float energy = texelFetch(u_energy, ivec2(index % tex_size.x, index / tex_size.x), 0).r;
    float alpha = 0.7;


    ivec2 cd = ivec2(v_texCoords.x * tex_size.x, v_texCoords.y * tex_size.y);
    vec2 cell_center = (cd + vec2(0.5)) / vec2(tex_size);
    alpha = 1.0 - length(vec2(v_texCoords - cell_center) * tex_size) ;

    if ( index < 0 ) { // background
        out_color = vec4(0.1, 0.1, 0.1, 0.1);
        return;
    }

    float d = length(vec2(cd - u_selected));

    ///
    if (0 <= d && d < 1) {
        //out_color = vec4(0.5, 1.0, 0.5, 0.95);
        alpha = 1.0;
    }

    //out_color = vec4(0, 0, 0, alpha);

    //vec4 p = texture(u_energy, v_texCoords);
    if ( (u_showLayer & 8) != 0 ) {
        // draw entire gen in cell
        ivec2 genSize = textureSize(u_gen, 0); // 70 x totalcellcount
        vec2 withinCell = vec2(v_texCoords - (cd / vec2(tex_size))) * vec2(tex_size);
        // cast to gen array index
        // fill square
        int genSide = int(floor(sqrt(float(genSize.x))));
        ivec2 withinGenArray = ivec2(withinCell * vec2(genSide));
        int genIndex = withinGenArray.x + genSide * withinGenArray.y;
        //out_color.r = withinGenArray.x / 8.0;
        //out_color.b = withinGenArray.y / 8.0;

        //for ( int x = 0; x < gen_size.x; x++ ) {

        //sum += texelFetch(u_gen, ivec2(x , index), 0).r;
        //}

        //out_color.g = texelFetch(u_gen, ivec2(genIndex, index), 0).r % 6 / 6.;
        //out_color.g = float(genIndex) / float(genSize.x);
        //out_color.b = withinCell.y;
        out_color = EncodeFloatRGBA(texelFetch(u_gen, ivec2(genIndex, index), 0).r / 2.0);
        out_color.a = 1.0;
        //texelFetch(u_gen, ivec2(v_texCoords.y * gen_size.y, index), 0);


        //out_color = texelFetch(u_gen, ivec2(index % gen_size.x, v_texCoords.y * gen_size.x), 0);

    }

    if ( (u_showLayer & 1) != 0 ) {
        out_color.r = 1.0 - age;
        out_color.a = alpha;
    }

    if ( (u_showLayer & 2) != 0 ) {
        out_color.b = energy;
        out_color.a = alpha;
    }

    //vec4 p = texture(u_energy, v_texCoords);
    if ( (u_showLayer & 4) != 0 ) {
        out_color.a = alpha;
        if ( energy - age > 0 ) {
            out_color.g = age;
        } else {
            out_color.g = 0.;
            out_color.r = age - energy;
        }
    }

}