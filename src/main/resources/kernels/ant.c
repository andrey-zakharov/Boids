#ifndef MAX_LOOKUP_ANGLE
    #define MAX_LOOKUP_ANGLE 30.
#endif

// Moore neighborhood 1 level
__constant float2 d[8] = {
	(float2)(-1., -1.), (float2)(0., -1.), (float2)(1., -1.),
	(float2)(-1.,  0.), 			       (float2)(1.,  0.),
	(float2)(-1., +1.), (float2)(0., +1.), (float2)(1., +1.),
};

// Moore neighborhood 2 level
__constant float2 dd[24] = {
	(float2)(-2., -2.), (float2)(-1., -2.), (float2)(0., -2.), (float2)(1., -2.), (float2)(2., -2.),
	(float2)(-2., -1.), (float2)(-1., -1.), (float2)(0., -1.), (float2)(1., -1.), (float2)(2., -1.),
	(float2)(-2., 0.), (float2)(-1., 0.),                   (float2)(1., 0.), (float2)(2., 0.),
	(float2)(-2., 1.), (float2)(-1., 1.), (float2)(0., 1.), (float2)(1., 1.), (float2)(2., 1.),
	(float2)(-2., 2.), (float2)(-1., 2.), (float2)(0., 2.), (float2)(1., 2.), (float2)(2., 2.),
};

enum State { empty = 0, full = 1 } ;
enum CellType { cellEmpty = 0, nest = 1, food = 2, obstacle = 3 } ;

float random (float2 _st) {
    float rem;
    return fract(sin(dot(_st.xy, (float2)(12.9898,78.233))) * 43758.5453123, &rem);
}

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

    float min_dot = cos(radians(MAX_LOOKUP_ANGLE));
    const float nextlen = length( np - origin );
    float maxlen = length(vel);

    // special case when velocity is zero
    if ( maxlen == 0.0 ) {
        min_dot = -M_PI;
        maxlen = 1.5; // 1 and 1.4 neighbours, Moore neighborhood
    }

    float dt = dot( normalize( vel ), normalize( np - origin ));
    bool valid = dt >= min_dot && nextlen <= maxlen;
#ifdef DEBUG_PATHFIND
    printf(
        "valid: %d np=(%.1f, %.1f) v=(%.2f, %.2f)\tnextlen=%f,\tmaxlen=%f, dot = %f\t>= min_dot=%f\t = %d%d\n",
        valid, np.x, np.y, vel.x, vel.y, nextlen, maxlen, dt, min_dot, dt >= min_dot, nextlen <= maxlen);
#endif

    return valid;
}

int get_array_index(uint w, uint h, float2 cell) {
    float2 pos = wrap_bounds(cell, w, h);
    return (int)pos.x + (int)pos.y * w;
}

bool samecell(float2 a, float2 b) {
    return (int)a.x == (int)b.x && (int)a.y == (int)b.y;
}
bool isequali(int2 a, int2 b) {
    return a.x == b.x && a.y == b.y;
}

// get force according to ground cell
float2 bfs_visit_cell_ground(bool emp, float2 cell_vec, enum CellType ground) {
#ifdef DEBUG
    printf(" ground bfs visit %.1f, %.1f = %d\n", cell_vec.x, cell_vec.y, ground);
#endif
    // accumulate all changes from found cell
    // real kernel is here TBD compose it
    float2 force = { 0., 0. };
    switch(ground) {
        case empty: return force;
        case nest:
            if ( !emp ) {
                force = cell_vec;
                //printf("nest %f, %f\n", force.x, force.y);
            } else {
                force = -cell_vec / (float2)5.;
            }
            break;

        case food:
            if ( emp ) force = cell_vec;
            else force = -cell_vec / (float2)5.;
            break;
        case obstacle:
            force = -cell_vec;
                        //vel = convert_float2(origin - cp);
                        //vel = (float2)(0., 0.);
                        // skip further bfs
                        // to prevent scanning food behind walls
            float len = length(force); // from 1 .. 7
            if ( len > 0) force /= len;
            break;
    }

    return force;
}

// // get force according to pher cell
float2 bfs_visit_cell_pheromones(float2 cell_vec, float attract, float repulse) {
    float2 f = { 0., 0. };
    if ( attract == 0 && repulse == 0 ) return f;

    if ( attract ) {
        f = cell_vec / (float2)(attract+1.);
            //printf("Found food trail force = %f, %f\n", f.x, f.y);
    }

    if ( repulse ) {
        f = -cell_vec * (float2)(repulse/25.);
    }

    return f;
}

typedef struct {
    // global
    uint w;
    uint h;
    uint sq;
    __global uchar*     ground;
    __global float*     pheromones;
    // ant
    bool emp;
    float2 raw_pos;
    float2 cell_mid;// center pos
    int2 cell;

    /// forces from ground
    /// pher #1
    /// pher #2
    float2 forces[3];// = { { 0., 0. }, { 0., 0. }, { 0., 0. } };
} ForceAccum;

void accum_init(ForceAccum* a) {
    a->forces[0] = (float2)0.;//{ 0., 0. }, { 0., 0. }, { 0., 0. } };
    a->forces[1] = (float2)0.;//{ 0., 0. }, { 0., 0. }, { 0., 0. } };
    a->forces[2] = (float2)0.;//{ 0., 0. }, { 0., 0. }, { 0., 0. } };
    a->cell = convert_int2_rtn(a->raw_pos);
    a->cell_mid = convert_float2_rtn(a->cell) + (float2)0.5; //  center of current cell
    a->sq = a->w * a->h;
}

void accum_visit(ForceAccum* a, float2 cell_vec) {
    float2 target = a->cell_mid + cell_vec;
    a->forces[0] += bfs_visit_cell_ground(a->emp, cell_vec, a->ground[get_array_index(a->w, a->h, target)]);
    float pher = a->pheromones[get_array_index(a->w, a->h, target)];
    float foodph = a->pheromones[get_array_index(a->w, a->h, target) + a->sq];

    if ( !a->emp ) {
        a->forces[1] += bfs_visit_cell_pheromones(cell_vec, pher, 0.);
    } else {
        a->forces[1] += bfs_visit_cell_pheromones(cell_vec, foodph, pher);
    }
}

#define KERNELS_ARGS     float time,\
                         float delta,\
                         float max_speed,\
                         uint w,\
                         uint h,\
                         __global uchar*     groundArray,\
                         __global float*     pheromonesArray,\
                         int ants_count,\
                         __global float2*    coords,\
                         __global float2*    velocities,\
                         __global uchar*     state

    // adapter to be done
#define pos coords[index]
#define vel velocities[index]
#define ant_state state[index]
#define isEmpty (ant_state == empty)
#define rand (random(pos + vel + (float2)(index+1) * (float2)time + (float2)(++_random_call)))

__kernel
void ant_moves(KERNELS_ARGS) {
    int index = get_global_id(0);
    int _random_call = 0.;
    float len = length(vel);
    if (len == 0) {
        return;
    }

    // special case - diggin out from burried
    enum CellType groundNow = groundArray[get_array_index(w, h, pos)];
    if ( groundNow == obstacle ) {
        pos += normalize(vel) / (float2)2.;
        return;
    }

    float2 next_pos = pos + normalize(vel);

    if ( !samecell(next_pos, pos) ) { // check valid moves
        enum CellType groundAhead = groundArray[get_array_index(w, h, next_pos)];
        switch( groundAhead ) {
            case obstacle:
                // special case when ant becomes buried, try to get away
                // it could be when ground suddenly changed
                vel = -vel * (float2)0.1;
                //vel = (float2)0.;

                return;
            case food:
                if ( isEmpty ) {
                    pos = next_pos;
                    vel = (float2)0.;
                    ant_state = full;
                    groundArray[get_array_index(w, h, next_pos)] = empty;
                    // TBD event ant.TakeEvent
                    return;
                } // ignore otherwise
                break;
            case nest:
                if ( !isEmpty ) {
                    pos = next_pos;
                    vel = (float2)0.;
                    ant_state = empty;
                    // TBD callback to nest nest.TakeEvent
                    return;
                } // ignore otherwise
                break;
            default:
                break;
        }

        // Leave pheromones
        __global float* arr = pheromonesArray;
        if ( !isEmpty ) arr = pheromonesArray + w * h;

        float current_pher = arr[get_array_index(w, h, pos)];
        for ( uint i = 0; i < sizeof(d)/sizeof(int2); i++ ) {
            current_pher += arr[get_array_index(w, h, pos+d[i])];
        }

        arr[get_array_index(w, h, pos)] += 0.1;//mix(current_pher / (float)9., (float)1., (float)0.25);
    }

    pos = wrap_bounds(next_pos, w, h);
}


__kernel
void ant_kernel(KERNELS_ARGS) {
    int index = get_global_id(0);
    float accel = max_speed / 10.;
    int _random_call = 0.;
    float2 max_velocity = normalize(vel) * max_speed;
#ifdef DEBUG
    printf(" = STEP pos=(%.3f, %.3f) vel=(%.3f, %.3f)\n", pos.x, pos.y, vel.x, vel.y);
#endif

    //uchar out_state = state[index];
    bool emp = ant_state == empty;

    queue cells;
    queue_init(&cells);
    queue_push(&cells, convert_int2_rtn(pos));

    ForceAccum accum;// = { w, h, groundArray, pheromonesArray, ant_state == empty, pos, iorigin, origin };
    accum.w = w;
    accum.h = h;
    accum.ground = groundArray;
    accum.pheromones = pheromonesArray;
    accum.emp = emp;
    accum.raw_pos = pos;
    accum_init(&accum);

    //0 - forceFromGround
    //accum_visit(&accum, accum.cell_mid - pos);
    //forces[0] += bfs_visit_cell_ground(emp, iorigin - pos, groundArray[get_array_index(w, h, iorigin)]);

    /// BFS
    while(!queue_empty(&cells)) {
        int2 cp = queue_pop(&cells);
#ifdef DEBUG_PATHFIND
        printf(" = looking around cell %d, %d\n", cp.x, cp.y);
#endif
        float2 fcp = convert_float2_rtn(cp) + (float2)0.5;
        float2 cell_vec = fcp - accum.raw_pos;

        /// get_valid_cells
        // neighbours
        for ( uint i = 0; i < sizeof(d)/sizeof(int2); i++ ) {
        	float2 fupp = fcp + d[i];

            //set_cell_pheromone(&pheromones, fupp, debug);
            int2 cell_to_add = convert_int2_rtn(fupp);

            if (check_valid(accum.cell_mid, max_velocity, fupp) && !check_queued(&cells, cell_to_add)) {
                queue_push(&cells, cell_to_add);
            }
        }

#ifdef DEBUG_PATHFIND
        printf("After scan neighbours: ");
        queue_print(&cells);
#endif
        accum_visit(&accum, cell_vec);
    }

#ifdef DEBUG
        printf("Applying forces on cell (%f, %f)\n", pos.x, pos.y);
        for( int i = 0; i < 3; i++ ) {
            float l = length(accum.forces[i]);
            if ( l == 0. ) {
                continue;
            }
            // if ( i == 0 ) printf( i == 0 ? "ground" : "pher", )
            printf("  forces[%d]: Vector(%0.5f,\t%0.5f\t).mod=%0.5f\n",
                //i == 0 ? "ground" : (i == 1 ? "pher" : "pherfood"),
                i,
                accum.forces[i].x, accum.forces[i].y, length(accum.forces[i]));
        }
#endif


#ifdef DEBUG
    for( size_t i = 0; i < cells.pos_tail; i++ ) {
        // debug
        //pheromonesArray[get_array_index(w, h, convert_float2(cells.queue[i]))] = food_trail;
    }
#endif
        // velocity accelerations
    vel += accum.forces[0];  //ground
    vel += accum.forces[1];

    // random fluctuation
    float r = (rand - 0.5) * 0.17453292519943 /*10 deg*/;
    //r = 0.01;
    vel = (float2)( vel.x * cos(r) - vel.y * sin(r), vel.x * sin(r) + vel.y * cos(r) );

    //debug queue

    float len = length(vel);
    if (len == 0) {
        // add random length rotated vector
        float2 r = (float2)(0., accel);
        float a = rand * M_PI * 2;
        r = (float2)( r.y * sin(a), r.y * cos(a));
        vel += r;
    } else if (len < max_speed) {
        // accelerate
        vel += normalize(vel) * accel;
    } else if (len > max_speed) {
        vel = normalize(vel) * max_speed;
    }
}

