/*
 * Copyright 2011 <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ocpsoft.rewrite.servlet.config.parameters;

import javax.servlet.ServletContext;

import com.ocpsoft.rewrite.config.Configuration;
import com.ocpsoft.rewrite.config.ConfigurationBuilder;
import com.ocpsoft.rewrite.config.Direction;
import com.ocpsoft.rewrite.context.EvaluationContext;
import com.ocpsoft.rewrite.servlet.config.Forward;
import com.ocpsoft.rewrite.servlet.config.HttpConfigurationProvider;
import com.ocpsoft.rewrite.servlet.config.HttpOperation;
import com.ocpsoft.rewrite.servlet.config.Path;
import com.ocpsoft.rewrite.servlet.config.RequestParameter;
import com.ocpsoft.rewrite.servlet.config.Response;
import com.ocpsoft.rewrite.servlet.config.SendStatus;
import com.ocpsoft.rewrite.servlet.config.parameters.binding.Request;
import com.ocpsoft.rewrite.servlet.http.event.HttpServletRewrite;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 */
public class ParameterConfigurationProvider extends HttpConfigurationProvider
{
   @Override
   public int priority()
   {
      return 0;
   }

   @Override
   public Configuration getConfiguration(final ServletContext context)
   {
      Configuration config = ConfigurationBuilder.begin()

               /*
                * Handle a request with Path parameter binding
                */
               .defineRule()
               .when(Direction.isInbound().and(
                        Path.matches("/{user}/order/{oid}")
                                 .where("user").matches("[a-zA-Z]+").bindsTo(Request.parameter("uname"))
                                 .where("oid").matches("[0-9]+").bindsTo(Request.parameter("oid"))
                        ))
               .perform(new HttpOperation() {
                  @Override
                  public void performHttp(final HttpServletRewrite event, final EvaluationContext context)
                  {
                     Response.addHeader("User-Name", event.getRequest().getParameter("uname")).perform(event, context);
                     Response.addHeader("Order-ID", event.getRequest().getParameter("oid")).perform(event, context);
                  }
               }.and(SendStatus.code(200)))

               /*
                * Forward a request to another resource
                */
               .defineRule()
               .when(Direction.isInbound().and(Path.matches("/p/{project}/story/{id}")
                        .where("project").matches("[a-zA-Z]+")
                        .where("id").matches("[0-9]+")))
               .perform(Forward.to("/viewProject?project={project}&id={id}"))

               /*
                * Handle the forwarded request
                */
               .defineRule()
               .when(Direction.isInbound().and(Path.matches("/viewProject"))
                        .and(RequestParameter.exists("project"))
                        .and(RequestParameter.exists("id")))
               .perform(SendStatus.code(200))

               /*
                * Handle a request that fails to bind
                */
               .defineRule()
               .when(Direction.isInbound().and(
                        Path.matches("/{user}/profile")
                                 .where("user").matches("[a-zA-Z]+").bindsTo(new MockFailedBinding())
                        ))
               .perform(new HttpOperation() {
                  @Override
                  public void performHttp(final HttpServletRewrite event, final EvaluationContext context)
                  {
                     Response.addHeader("User-Name", event.getRequest().getParameter("uname")).perform(event, context);
                     Response.addHeader("Order-ID", event.getRequest().getParameter("oid")).perform(event, context);
                  }
               }.and(SendStatus.code(200)));

      return config;
   }
}
