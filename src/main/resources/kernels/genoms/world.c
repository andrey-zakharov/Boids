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
    barrier(CLK_LOCAL_MEM_FENCE);
    // get cell
    float amount = TOP_LIGHT;
    if ( group > 0 ) {
        amount = 0.99 * light[index-h];
    }
    light[index] = amount;
}