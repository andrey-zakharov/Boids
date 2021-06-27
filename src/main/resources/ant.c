#ifndef MAXLOOKUPANGLE
    #define MAXLOOKUPANGLE 30.
#endif

__constant float2 d[8] = {
	(float2)(-1., -1.), (float2)(0., -1.), (float2)(1., -1.),
	(float2)(-1.,  0.), 			       (float2)(1.,  0.),
	(float2)(-1., +1.), (float2)(0., +1.), (float2)(1., +1.),
};

__constant const size_t StateEmpty = 0;
__constant const size_t StateFull = 0;
enum State { empty = 0, full = 1 } ;
enum CellType { cellEmpty = 0, nest = 1, food = 2, obstacle = 3 } ;

float random (float2 _st) {
    float rem;
    return fract(sin(dot(_st.xy, (float2)(12.9898,78.233))) * 43758.5453123, &rem);
}

typedef struct {
    __global float* matrix;
    uint w;
    uint h;
} wrapping_field;

float2 wrap_bounds(float2 pos, uint w, uint h) {
    float2 ret = pos;
    if (ret.x < 0 ) ret.x += w;
    else if (ret.x > w ) ret.x -= w;
    if (ret.y < 0) ret.y += h;
    else if ( ret.y > h ) ret.y -= h;
    return ret;
}

/// origin of ant, cp - current point, np - next point to check
bool check_valid(const float2 origin, const float2 vel, const float2 np) {

    float min_dot = cos(radians(MAXLOOKUPANGLE));
    const float nextlen = length( np - origin );
    float maxlen = length(vel);
    // special case when velocity is zero
    if ( maxlen == 0.0 ) {
        min_dot = -M_PI;
        maxlen = 1.5; // 1 and 1.4 neighbours
    }

    float dt = dot( normalize( vel ), normalize( np - origin ));
    printf("v=(%.3f, %.3f)\tnextlen=%f,\tmaxlen=%f, dot = %f\t>= min_dot=%f\t = %d\n", vel.x, vel.y, nextlen, maxlen, dt, min_dot, dt >= min_dot);

	if ( dt < min_dot ) return false;
    if ( nextlen > maxlen ) return false;
    return true;
}

//wrapping safe setter
void set_cell_pheromone(wrapping_field* pheromones, float2 cell, enum PherType v ) {
    float2 pos = wrap_bounds(cell, pheromones->w, pheromones->h);
    int idx = (int)pos.x + (int)pos.y * pheromones->w;
    /*switch(v) {
        case trail: pheromones->matrix[idx] = 1.0f;
        case food_trail: pheromones->matrix[idx] = -1.0f;
        case debug: pheromones->matrix[idx] = -2.0f;
        default: return;
    }*/
    pheromones->matrix[idx] = (float)v;
}

__kernel
void ant_kernel(
    float delta,
    float max_speed,
    uint w,
    uint h,
    __global __read_only uchar*     ground,
    __global float*     pheromones,
    __global float2*    coords,
    __global float2*    velocities,
    __global uchar*     state
)
{
    float accel = max_speed / 10.;

    int index = get_global_id(0);
    // test 1
    // coords[index] = wrap_bounds(coords[index] + (float2)(1.0, 1.0), w, h);
    //velocities[index] = velocities[index]  + max_speed;
    //    return;
    float2 pos = coords[index];
    float2 vel = velocities[index];
    printf(" = STEP pos=(%.3f, %.3f) vel=(%.3f, %.3f)\n", pos.x, pos.y, vel.x, vel.y);
    uchar out_state = state[index];
    wrapping_field phers = {pheromones, w, h};
    bool emp = state[index] == 0;

    // random fluctuation
    float r = (random(pos+vel) - 0.5) * 0.17453292519943 /*10 deg*/;
    //r = 0.01;
    vel = (float2)( vel.x * cos(r) - vel.y * sin(r), vel.x * sin(r) + vel.y * cos(r) );

    queue cells;
    queue_init(&cells);
    queue_push(&cells, convert_uint2(pos));
    uint2 origin = cells.queue[0];
    // and center of current cell
    float2 iorigin = convert_float2(origin) + (float2)0.5;
    size_t obstacles_found = 0;
    float2 vel_acc = (float2)0.;

    /// BFS
    while(!queue_empty(&cells)) {
        uint2 cp = queue_pop(&cells);
        //printf("= looking around cell %d, %d\n", cp.x, cp.y);

        float2 fcp = convert_float2(cp) + (float2)0.5;


        // neighbours
        // first check by min angle, but at least get one with max dot product
        size_t added = 0;
        float maxdot = -100.;
        uint maxdoti = -1;
        for ( uint i = 0; i < sizeof(d)/sizeof(int2); i++ ) {
        	float2 fupp = fcp + d[i];

            set_cell_pheromone(&phers, fupp, debug);
            uint2 cell_to_add = convert_uint2(fupp);

            if (check_valid(iorigin, vel, fupp) && !check_queued(&cells, cell_to_add)) {
                queue_push(&cells, cell_to_add);
                added ++;
            }

            float d = dot( normalize( vel ), normalize( fupp - iorigin ) );
            if ( d > maxdot ) {
                maxdot = d;
                maxdoti = i;
            }
        }

        //printf("added to PF queue= %d\n", added);
        //queue_print(&cells);

#ifdef WITH_FALLBACK_PATHFINDING
        if ( added == 0 ) {
            uint2 last_chance = fcp + d[maxdoti];
            if ( length (last_change - fcp) < length(vel) ) {
                //debug
                set_cell_pheromone(&phers, last_chance, food_trail);
                queue_push(&cells, last_chance);
            }
        }
#endif

        //printf("|| end looking around cell\n");
        if ( cp.x == origin.x && cp.y == origin.y ) {
            continue;
        }

        // accumulate all changes from found cell
        if ( ground[ cp.x + cp.y * w ] == obstacle ) {
            obstacles_found++;
            float2 opforce = convert_float2(origin) - convert_float2(cp);
            float len = length(opforce); // from 1 .. 7
            vel_acc += (opforce / len);
            //vel = convert_float2(origin - cp);
            //vel = (float2)(0., 0.);
            // skip further bfs
            // to prevent scanning food behind walls
        }
        /*if ( pheromones[ cp.x + cp.y * w ] > 0 ) {
            vel += 3.f / vel - convert_float2(cp - origin);
        }*/
    }

    if (obstacles_found > 0) {
        vel += vel_acc / obstacles_found;
        printf("obstacles: %d vel addition %0.5f, %0.5f\n", obstacles_found, vel_acc.x, vel_acc.y);
    }

    out_state = obstacles_found; // ? obstacle : empty;

    set_cell_pheromone(&phers, pos, trail);
    //debug queue
    for( size_t i = 0; i < cells.pos_tail; i++ ) {
        // debug
        set_cell_pheromone(&phers, convert_float2(cells.queue[i]), food_trail);
    }

    float len = length(vel);
    if (len == 0) {
        // add random length rotated vector
        float2 r = (float2)(0., accel);
        float a = random(pos+vel) * M_PI * 2;
        r = (float2)( r.y * sin(a), r.y * cos(a));
        vel += r;
    } else if (len < max_speed) {
        // accelerate
        vel += normalize(vel) * accel;
    } else if (len > max_speed) {
        vel = normalize(vel) * max_speed;
    }

    velocities[index] = vel;
    float2 next_coords = wrap_bounds(pos + velocities[index] * delta, w, h);
    uint2 next_pos = convert_uint2(next_coords);
    //if ( ground[ next_pos.x + next_pos.y * w ] == obstacle ) {
    //    velocities[index] = -velocities[index];
//
    //} else {
        coords[index] = next_coords;
    //}
    state[index] = out_state;
}
