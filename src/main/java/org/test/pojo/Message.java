package org.test.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** A message object that Flink recognizes as a valid POJO. */
@JsonIgnoreProperties(ignoreUnknown=true)
public class Message {
//this string user is the field name in the datastream that will be transformed into OCSF format.
// currently the first field in our json string is @version so i am using it for testing.
//when our flink job sees @version then it transforms it to "itWorkedOSF"
@JsonProperty("@version")
public String itWorkedOCSF;

}
