package se.ithuset.ailabb.tools;

import org.springframework.stereotype.Component;

import dev.langchain4j.agent.tool.Tool;
import se.ithuset.ailabb.domain.Departure;

@Component
public class TrafficDataTool {

    @Tool("Use this tool to find the next departure from an origin to a destination")
    public Departure getNextDeparture(String origin, String destination) {
        //Call the traffic data api and return the goodies
        return null;
    }
}
