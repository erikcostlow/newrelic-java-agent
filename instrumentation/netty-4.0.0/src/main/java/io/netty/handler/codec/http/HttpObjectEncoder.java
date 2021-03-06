/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.netty.handler.codec.http;

import com.agent.instrumentation.netty40.NettyUtil;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import io.netty.channel.ChannelHandlerContext_Instrumentation;

import java.util.List;

@Weave(type = MatchType.BaseClass)
public class HttpObjectEncoder {

    // heading downstream
    protected void encode(ChannelHandlerContext_Instrumentation ctx, Object msg, List<Object> out) {
        boolean expired = NettyUtil.processResponse(msg, ctx.pipeline().token);
        if (expired) {
            ctx.pipeline().token = null;
        }
        Weaver.callOriginal();
    }

}
