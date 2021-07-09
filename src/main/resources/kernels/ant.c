#ifndef MAX_LOOKUP_ANGLE
    #define MAX_LOOKUP_ANGLE 30.
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
        maxlen = 1.5; // 1 and 1.4 neighbours
    }

    float dt = dot( normalize( vel ), normalize( np - origin ));
    //printf("check_valid v=(%.3f, %.3f)\tnextlen=%f,\tmaxlen=%f, dot = %f\t>= min_dot=%f\t = %d\n", vel.x, vel.y, nextlen, maxlen, dt, min_dot, dt >= min_dot);

	if ( dt < min_dot ) return false;
    if ( nextlen > maxlen ) return false;
    return true;
}

int get_array_index(uint w, uint h, float2 cell) {
    float2 pos = wrap_bounds(cell, w, h);
    return (int)pos.x + (int)pos.y * w;
}

bool samecell(float2 a, float2 b) {
    return (int)a.x == (int)b.x && (int)a.y == (int)b.y;
}
bool isequali(uint2 a, uint2 b) {
    return a.x == b.x && a.y == b.y;
}

float2 bfs_visit_cell_ground(bool emp, float2 cell_vec, enum CellType ground) {
    // accumulate all changes from found cell
    // real kernel is here TBD compose it
    float2 force = { 0., 0. };
    switch(ground) {
        case empty: return force;
        case nest:
            force = -cell_vec;
            if ( !emp ) force *= (float2)-1.;
            break;

        case food:
            force = cell_vec;
            if ( !emp ) force *= (float2)-1.;
            break;
        case obstacle:
//                obstacles_found++;
            force = cell_vec;
                        //vel = convert_float2(origin - cp);
                        //vel = (float2)(0., 0.);
                        // skip further bfs
                        // to prevent scanning food behind walls
            break;
    }

    float len = length(force); // from 1 .. 7
    if ( len > 0) return (force / len);
    return force;
}

float2 bfs_visit_cell_pheromones(bool emp, float2 cell_vec, float ph) {
    float2 f = { 0., 0. };
    if ( ph == none ) return f;

    if ( ph < 0 ) { // food trail
        if ( emp ) { // power boost toward food!
            f = cell_vec / (float2)-ph;
            //printf("Found food trail force = %f, %f\n", f.x, f.y);
        }
        // else go according to simple trails
        //force = iorigin - fcp;
    } else { // trail
        if ( emp ) {
            f = -cell_vec * (float2)ph;

        } else {
            f = cell_vec;
        }
    }

    return f;
}

__kernel
void ant_kernel(
    float delta,
    float max_speed,
    uint w,
    uint h,
    __global uchar*     groundArray,
    __global float*     pheromonesArray,
    __global float2*    coords,
    __global float2*    velocities,
    __global uchar*     state
)
{
    float accel = max_speed / 10.;

    int index = get_global_id(0);
    // adapter to be done
#define pos coords[index]
#define vel velocities[index]
#define ant_state state[index]

    //float2 pos = coords[index];
    //float2 vel = velocities[index];
    //printf(" = STEP pos=(%.3f, %.3f) vel=(%.3f, %.3f)\n", pos.x, pos.y, vel.x, vel.y);
    //uchar out_state = state[index];

    bool emp = ant_state == empty;
    // check step
    float2 next_pos = pos + vel * delta;
    if ( !samecell(next_pos, pos) ) {
        enum CellType groundAhead = groundArray[get_array_index(w, h, next_pos)];
        switch( groundAhead ) {
            case obstacle:
                vel = -vel * (float2)0.1;
                return;
            case food:
                if ( emp ) {
                    pos = next_pos;
                    vel = (float2)0.;
                    ant_state = full;
                    groundArray[get_array_index(w, h, next_pos)] = empty;
                    // TBD event ant.TakeEvent
                    return;
                } else {
                    vel = -vel * (float2)0.1;
                    return;
                }
                break;
            case nest:
                if ( !emp ) {
                    pos = next_pos;
                    vel = (float2)0.;
                    ant_state = empty;
                    // TBD callback to nest nest.TakeEvent
                    return;
                } else {
                    vel = -vel * (float2)0.1;
                    return;
                }
                break;
            default:
                break;
        }
    }
    //switch()

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
//    size_t obstacles_found = 0;
    float2 vel_acc = (float2)0.;

    /// BFS
    while(!queue_empty(&cells)) {
        uint2 cp = queue_pop(&cells);
        //printf("= looking around cell %d, %d\n", cp.x, cp.y);

        float2 fcp = convert_float2(cp) + (float2)0.5;
        float2 cell_vec = fcp - iorigin;

        /// get_valid_cells
        // neighbours
        // first check by min angle, but at least get one with max dot product
        size_t added = 0;
        float maxdot = -100.;
        uint maxdoti = -1;
        for ( uint i = 0; i < sizeof(d)/sizeof(int2); i++ ) {
        	float2 fupp = fcp + d[i];

            //set_cell_pheromone(&pheromones, fupp, debug);
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
        if ( isequali(cp.x, origin.x) ) {
            continue;
        }

        float2 forceFromGround = bfs_visit_cell_ground(emp, cell_vec, groundArray[get_array_index(w, h, fcp)]);
        float2 forceFromPheromon = bfs_visit_cell_pheromones(emp, cell_vec, pheromonesArray[get_array_index(w, h, fcp)]);

#ifdef DEBUG
        if ( length(forceFromGround) > 0)
            printf("forced from ground: Vector(%0.5f,\t%0.5f\t).mod=%0.5f\n",
                forceFromGround.x, forceFromGround.y, length(forceFromGround));
        if ( length(forceFromPheromon) > 0)
            printf("forced from phers: Vector(%0.5f,\t%0.5f\t).mod=%0.5f\n",
                forceFromPheromon.x, forceFromPheromon.y, length(forceFromPheromon));
#endif
        vel_acc += forceFromGround; // obstacles_found
        vel_acc += forceFromPheromon;
    }


    vel += vel_acc ;//*(float2)(2.)  / obstacles_found;


#ifdef DEBUG
    for( size_t i = 0; i < cells.pos_tail; i++ ) {
        // debug
        pheromonesArray[get_array_index(w, h, convert_float2(cells.queue[i]))] = food_trail;
    }
#endif

    if (random(pos+vel) > 0.7) {
        enum PherType trailToLeave = trail;
        if ( !emp ) trailToLeave = food_trail;
        pheromonesArray[get_array_index(w, h, pos)] = trailToLeave;
    }
    //debug queue

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

    //velocities[index] = vel;
    pos = wrap_bounds(pos + vel / 10 /* delta * 2*/, w, h);
    //}
    //state[index] = out_state;
}

