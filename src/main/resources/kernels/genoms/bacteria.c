#ifndef max_age
    #define max_age 10000.
#endif

#ifndef max_energy
    #define max_energy 100.
#endif

#ifndef energy_ps // photosynthesis
    #define energy_ps 0.008
#endif

#ifndef minerals_to_energy
    #define minerals_to_energy 0.5
#endif

#define NEIB_COUNT 4

__constant float2 d[NEIB_COUNT] = { (float2)(0, -1.),
	(float2)(-1., 0.),                      (float2)(+1., 0.),
                            (float2)(0, 1.) };

#define KERNELS_ARGS     float time,\
                         float delta,\
                         int w,\
                         int h,\
                         __global int*     items_total,\
                         __global int*     pos,\
                         __global uchar*     gen,\
                         __global uchar*       current_command,\
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
    if ( bacteria_field[field_idx] != index ) {
        printf("%0.3f broken %d - %d %d field = %d\n", time, index, x, y, bacteria_field[field_idx]);
    }

    if (age[index] > 1.) {
        // die
        bacteria_field[field_idx] = -1;
        if ( ground[field_idx] == empty ) {
            ground[field_idx] = food;
        }
        //printf("%0.3f died: %d (%d x %d) %0.3f\n", time, index, x, y, age[index]);
        //atomic_inc(&items_total[1]);
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

    if ( ground[field_idx] == food /* food */ ) {
        energy[index] += 0.5;
        ground[field_idx] = empty;
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

    /// division
    if ( energy[index] >= 1. ) {

        //printf("%d", ground[0 + 10*w]);
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
                    if ( random(idx + time + i) > 0.99 ) {
                        gen[gen_len * idx + i] = (random(idx + time+1. + i) * COMMAND_COUNT);
                    } else {
                        gen[gen_len * idx + i] = gen[gen_len * index + i];
                    }
                }
                                       /* for( uint y = 0; y < h; y++ ) {
                                            for( uint x = 0; x < w; x++ ) {
                                                printf("%d\t", bacteria_field[x + y * w]);
                                            }
                                            printf("\n");
                                        }*/
                current_command[idx] = 0;
                age[idx] = 0.;
                energy[idx] = 0.1;
                energy[index] = 0.1;

                //printf("%0.3f new %d ->\t%d / %d\t%d x %d ->\t%d x %d\n", time, index, idx, w * h, x, y, nx, ny);

                break;
            }

        }
    }

    /// gen instructions
    const int cmd_count = 6;
    const int genArraySize = gen_len * w * h;
    int cmd_idx = (index*gen_len + current_command[index]);
    int cmd = gen[cmd_idx] % cmd_count;
    int dir = -1;
    switch(cmd) {
        case 0: // jmp
            current_command[index] = gen[(cmd_idx + 1) % genArraySize] % gen_len;
            break;
        case 1: // move random
            dir = random(index + time + cmd_idx) * NEIB_COUNT; // dir count
            current_command[index] = (current_command[index] + 1) % gen_len;
        case 2: // move
            {

                if ( cmd == 2 ) {
                    dir = gen[(cmd_idx + 1) % genArraySize] % NEIB_COUNT;
                    current_command[index] = (current_command[index] + 1) % gen_len;
                }

                if ( energy[index] < 0.01 ) break;

                int nx = x + d[dir].x;
                int ny = y + d[dir].y;
                if ( nx >= 0 && nx < w && ny >= 0 && ny < h ) {
                    int fld_idx = nx + ny*w;
                    if ( ground[fld_idx] != obstacle && bacteria_field[fld_idx] == -1 ) {
                        bacteria_field[fld_idx] = index;
                        bacteria_field[field_idx] = -1;
                        pos[2*index] = nx;
                        pos[2*index+1] = ny;
                        energy[index] -= 0.01;
                    }
                }
            }
            break;
        case 3: // eat_sun
            current_command[index] = (current_command[index] + 1 ) % gen_len;
            if ( light[field_idx] > 0 ) {
                float a = 0.2 * light[field_idx];
                energy[index] += a;
                light[field_idx] -= a;
            }
            break;
        case 4: // harvest minerals
            current_command[index] = (current_command[index] + 1 ) % gen_len;
            if ( minerals[field_idx] > 0. ) {
                energy[index] += minerals_to_energy * minerals[field_idx];
                minerals[field_idx] = 0.;
            }
            break;
        case 5: // eat neighbour
            {
                dir = gen[(cmd_idx + 1) % genArraySize] % NEIB_COUNT;
                current_command[index] = (current_command[index] + 2) % gen_len;

                int nx = x + d[dir].x;
                int ny = y + d[dir].y;
                if ( nx >= 0 && nx < w && ny >= 0 && ny < h ) {
                    int fld_idx = nx + ny*w;
                    if ( bacteria_field[fld_idx] != -1 ) {
                        //printf("#%d (%d x %d) eat #%d (%d x %d) dir=%d\n", index, x, y, bacteria_field[fld_idx], nx, ny, dir );
                        energy[index] += energy[bacteria_field[fld_idx]];
                        bacteria_field[fld_idx] = -1;
                    }
                }
            }
            break;

        default:
            break;
    }


}
