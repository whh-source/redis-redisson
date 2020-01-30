package com.whh.redisredisson;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RemoteImpl implements RemoteInterface {

    @Override
    public long hell() {
        log.info("hello bb");
        return 0;
    }
}
