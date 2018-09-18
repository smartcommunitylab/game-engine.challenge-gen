/**
 * Copyright 2011-2013 SAYservice s.r.l.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.sayservice.platform.smartplanner.data.message.otpbeans;

import java.util.ArrayList;
import java.util.List;

public class TransitStops {

    private String agency;
    private String id;

    private List<String> stopsId;

    public TransitStops() {
        stopsId = new ArrayList<String>();
    }

    public String getAgency() {
        return agency;
    }

    public void setAgency(String agency) {
        this.agency = agency;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<String> getStopsId() {
        return stopsId;
    }

    public void setStopsId(List<String> stopsId) {
        this.stopsId = stopsId;
    }

}
