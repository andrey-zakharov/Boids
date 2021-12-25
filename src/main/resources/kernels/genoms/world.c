#ifndef TOP_LIGHT
    #define TOP_LIGHT 100
#endif

#define KERNELS_ARGS     float time,\
                         float delta,\
                         uint w,\
                         uint h,\
                         __global float*     light,\
                         __global float*     minerals,\
                         __global float*     moisture,\
                         __global uchar*     cells

__kernel
void enlight(KERNELS_ARGS) {
    int index = get_global_id(0);
    int local_id = get_local_id(0);
    int group_size = get_local_size(0);
    int group = get_group_id(0);
    float K = 1. - 1./(float)h;

    // get cell
    float amount = 1.;
    if ( group > 0 ) {
        int cell_above_idx = index-w;
        amount = K * light[cell_above_idx];
        if ( cells[cell_above_idx] != 0 ) {
            amount /= 1.5;
        } else {

        }
    }
    barrier(CLK_LOCAL_MEM_FENCE);
    light[index] = amount;
}
