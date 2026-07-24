Feature: Step Comments Feature
#Feature: Step Comments Feature
  @comment_tag1 #a comment
  Scenario: Step Comments Scenario
#my_tag1 my_tag_2
    #before line comment First
    When First step
    #before comment for Second
    #before comment#2 for Second
    Then Second step #inline comment for Second
    But Third step
    #before line comment for Fourth
    And Fourth step with "#value"
    #after line comment
    #after line comment2
