-- This config example file is released into the Public Domain.

-- Get a location for all objects with addresses. If mapped as a polygon or
-- multipolygon, the centroid will be used.

local addrs = osm2pgsql.define_table({
    name = 'locations',
    ids = { type = 'any', id_column = 'osm_id', type_column = 'osm_type' },
    columns = {

        { column = 'id', sql_type = 'serial' , create_only=true },
        { column = 'text', type = 'text' },
        { column = 'geom', type = 'point', projection = 4326, not_null = true },
        { column = 'lat', create_only=true ,sql_type = 'real generated always as (st_y(st_transform(geom,4326))) stored' },
        { column = 'lng', create_only=true ,sql_type = 'real generated always as (st_x(st_transform(geom,4326))) stored' }
    }
})


local function flatten(tags)
    local addr = {}
    addr.text = ''
    local count = 0
    for key,value in pairs(tags) do 
        if (value) then
            addr.text = addr.text .. ' ' .. value
            count = count + 1
        end
    end

    return count > 1, addr
end
local function addPoint(addr, point) 
    if (point) then
        addr.geom = point
        addrs:insert(addr)
    end
end


function osm2pgsql.process_node(object)
    local any, addr = flatten(object.tags)
    if any then
        addPoint(addr,object:as_point())
    end
end

function osm2pgsql.process_way(object)
    local any, addr = flatten(object.tags)
    if any then
        addPoint(addr,object:as_linestring():centroid())
    end
end

function osm2pgsql.process_relation(object)
    local any, addr = flatten(object.tags)
    if any then
        addPoint(addr,object:as_geometrycollection():centroid())
    end
end
