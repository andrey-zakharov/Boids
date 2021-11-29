#ifndef max_age
    #define max_age 10000.
#endif
#ifndef gen_len
    #define gen_len 80
#endif

#ifndef max_energy
    #define max_energy 100.
#endif

#ifndef energy_ps // photosynthesis
    #define energy_ps 0.01
#endif


__constant float2 d[4] = { (float2)(0, -1.),
	(float2)(-1., 0.), (float2)(+1., 0.),
    (float2)(0, 1.) };

#define KERNELS_ARGS     float time,\
                         float delta,\
                         int w,\
                         int h,\
                         __global int*     items_total,\
                         __global int*     pos,\
                         __global float*     gen,\
                         __global int*       current_command,\
                         __global float*     age,\
                         __global float*     energy,\
                         __global int*       bacteria_field,\
                         __global float*     light,\
                         __global float*     minerals,\
                         __global float*     moisture,\
                         __global uchar*     ground

float random (float2 _st) {
    float rem;
    return fract(sin(dot(_st.xy, (float2)(12.9898,78.233))) * 43758.5453123, &rem);
}

// actually emitter too
__kernel
void graver(KERNELS_ARGS) {
    int index = get_global_id(0);
    int x = pos[2*index];
    int y = pos[2*index+1];
    int field_idx = x + y * w;
    if (bacteria_field[field_idx] == -1) return;

    if (age[index] > 1.) {
        // die
        ground[field_idx] = food;
        bacteria_field[field_idx] = -1;
        atomic_inc(&items_total[1]);
        return;
        // free index
    } else {
        age[index] += 1 / max_age;
    }

    if ( light[field_idx] > 0 ) {
        // 0.05 -- tbd from height
        float a = energy_ps * light[field_idx];
        energy[index] += a;
        light[field_idx] -= a;
    }
}

__kernel
void gen_processor(KERNELS_ARGS, int dx, int dy) {
    int roww = ceil(w / 3.);
    int y = (get_global_id(0) / roww) * 3 + dy;
    int x = (get_global_id(0) % roww) * 3 + dx;
    if ( x >= w || y >= h ) return;
    int field_idx = x + y * w;
    if (bacteria_field[field_idx] == -1) return;
    int index = bacteria_field[field_idx];
    if ( energy[index] < 1. ) return;

    // spend some energy for division, other - divide equally
    //find next place
    for ( uint i = 0; i < sizeof(d)/sizeof(int2); i++ ) {
        int nx = x + d[i].x;
        int ny = y + d[i].y;
        if ( nx < 0 || nx >= w || ny < 0 || ny >= h ) continue;
        int fld_idx = nx + ny*w;

        if ( ground[fld_idx] != obstacle && bacteria_field[fld_idx] == -1 ) {
            //clone(index, x + d[i].x, y + d[i].y);
            int idx = atomic_inc(&items_total[0]);

            // indexes
            bacteria_field[fld_idx] = idx;
            pos[2*idx] = nx;
            pos[2*idx+1] = ny;
            for ( size_t i = 0; i < gen_len; i++ ) {
                if ( random(idx + time) > 0.99 ) {
                    gen[gen_len * idx + i] = (random(idx + time+1.) * COMMAND_COUNT);
                } else {
                    gen[gen_len * idx + i] = gen[gen_len * index + i];
                }
            }
            current_command[idx] = 0;
            age[idx] = 0.;
            energy[idx] = 0.1;
            energy[index] = 0.1;
            //printf("new %d / %d\n", idx, w * h);
            break;
        }
    }
}
