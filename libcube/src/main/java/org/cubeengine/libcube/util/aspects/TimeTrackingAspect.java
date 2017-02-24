/**
 * This file is part of CubeEngine.
 * CubeEngine is licensed under the GNU General Public License Version 3.
 *
 * CubeEngine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * CubeEngine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with CubeEngine.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.cubeengine.libcube.util.aspects;

import co.aikar.timings.Timing;
import co.aikar.timings.Timings;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.plugin.PluginContainer;

import java.util.Optional;

@Aspect("pertarget(commands())")
public class TimeTrackingAspect {

    private Timing timing;

    @Pointcut("@annotation(org.cubeengine.butler.parametric.Command) && execution(* *(..))")
    private void commands() {
    }

    @Around("commands()")
    public Object trackTime(ProceedingJoinPoint pjp) throws Throwable {
        if (timing == null) {
            Optional<PluginContainer> cubeengine = Sponge.getGame().getPluginManager().getPlugin("cubeengine");
            if (cubeengine.isPresent()) {
                timing = Timings.of(cubeengine.get(), pjp.getSignature().getName());
            } else {
                return pjp.proceed();
            }
        }

        timing.startTimingIfSync();
        Object r = pjp.proceed();
        timing.stopTimingIfSync();

        return r;

    }

}
