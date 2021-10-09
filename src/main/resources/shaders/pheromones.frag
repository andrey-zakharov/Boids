#ifdef GL_ES
    precision mediump float;
#endif

varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform mat4 u_projTrans;
uniform int tex_index;

void main() {
        float pher =
            texture2D(u_texture, v_texCoords).r
        /*  +  texture2D(u_texture, vec2(v_texCoords.x-1., v_texCoords.y)).r +
            texture2D(u_texture, vec2(v_texCoords.x+1., v_texCoords.y)).r +
            texture2D(u_texture, vec2(v_texCoords.x, v_texCoords.y-1.)).r +
            texture2D(u_texture, vec2(v_texCoords.x, v_texCoords.y+1.)).r
        ) / 5.*/;
        vec4 color = vec4(0.0, 0.0, 0.0, 0.0);
        if (tex_index == 1) {
            color = vec4(0, 1., 0, pher);
        } else if ( tex_index == 0 ) {
            color = vec4(0, 0, 1., pher);
        }

        gl_FragColor = color;
}