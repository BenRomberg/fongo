package com.github.fakemongo.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import org.bson.types.Binary;
import org.bson.types.MaxKey;
import org.bson.types.MinKey;
import org.bson.types.ObjectId;
import org.junit.Test;

import com.github.fakemongo.Fongo;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import com.mongodb.DBRef;

@SuppressWarnings("javadoc")
public class ExpressionParserTest {

  @Test
  public void testSimpleAndFilter() {
    DBObject query = new BasicDBObject("a", 3).append("n", "j");
    List<DBObject> results = doFilter(
        query,
        new BasicDBObject("a", 3),
        new BasicDBObject("a", asList(1, 3)).append("n", "j"),
        new BasicDBObject("a", 3).append("n", "j"),
        new BasicDBObject("n", "j")
    );
    assertEquals(Arrays.<DBObject>asList(
        new BasicDBObject("a", asList(1, 3)).append("n", "j"),
        new BasicDBObject("a", 3).append("n", "j")
    ), results);
  }

  @Test
  public void nestedAllRegexFilter() {
    DBObject query = new BasicDBObject("_keywords", new BasicDBObject("$all", Arrays.asList("john", Pattern.compile("^doe"))));

    List<DBObject> results = doFilter(
        query,
        new BasicDBObject("_keywords", Arrays.asList("john", "more")),
        new BasicDBObject("_keywords", Arrays.asList("tim", "norton")),
        new BasicDBObject("_keywords", Arrays.asList("john", new BasicDBObject("doe", ""))),
        new BasicDBObject("_keywords", Arrays.asList("john", "doeson")),
        new BasicDBObject("_keywords", Arrays.asList("john", "don")),
        new BasicDBObject("_keywords", Arrays.asList("john", "doe"))
    );

    assertEquals(Arrays.<DBObject>asList(
        new BasicDBObject("_keywords", Arrays.asList("john", "doeson")),
        new BasicDBObject("_keywords", Arrays.asList("john", "doe"))
    ), results);
  }

  @Test
  public void topLevelAndFilter() {
    DBObject query = new BasicDBObject("$and", Arrays.asList(new BasicDBObject("a", 3), new BasicDBObject("b", 4)));

    List<DBObject> results = doFilter(
        query,
        new BasicDBObject("a", 3).append("b", 4),
        new BasicDBObject("a", 3).append("b", 5),
        new BasicDBObject("b", 4),
        new BasicDBObject("a", 3),
        new BasicDBObject("a", 5).append("b", 4)
    );

    assertEquals(Arrays.<DBObject>asList(
        new BasicDBObject("a", 3).append("b", 4)
    ), results);
  }

  @Test
  public void testBasicOperators() {
    assertQuery(new BasicDBObject("a", new BasicDBObject("$gte", 4)), Arrays.<DBObject>asList(
        new BasicDBObject("n", "stu").append("a", 4),
        new BasicDBObject("n", "tim").append("a", 5),
        new BasicDBObject("a", asList(3, 4))
    ));
    assertQuery(new BasicDBObject("a", new BasicDBObject("$lte", 3)), Arrays.<DBObject>asList(
        new BasicDBObject("n", "neil").append("a", 1),
        new BasicDBObject("n", "fred").append("a", 2),
        new BasicDBObject("n", "ted").append("a", 3),
        new BasicDBObject("a", asList(3, 4))
    ));
    assertQuery(new BasicDBObject("a", new BasicDBObject("$gt", 4)), Arrays.<DBObject>asList(
        new BasicDBObject("n", "tim").append("a", 5)
    ));
    assertQuery(new BasicDBObject("a", new BasicDBObject("$lt", 3)), Arrays.<DBObject>asList(
        new BasicDBObject("n", "neil").append("a", 1),
        new BasicDBObject("n", "fred").append("a", 2)
    ));
    assertQuery(new BasicDBObject("a", new BasicDBObject("$gt", 3).append("$lt", 5)), Arrays.<DBObject>asList(
        new BasicDBObject("n", "stu").append("a", 4),
        new BasicDBObject("a", asList(3, 4))
    ));
    assertQuery(new BasicDBObject("a", new BasicDBObject("$eq", 1)), Arrays.<DBObject>asList(
        new BasicDBObject("n", "neil").append("a", 1)
    ));
  }

  @Test
  public void testConditionalEmbeddedOperator() {

    DBObject query = new BasicDBObject("a.b", new BasicDBObject("$gt", 2));
    List<DBObject> results = doFilter(
        query,
        new BasicDBObject("a", asList(
            new BasicDBObject("b", 1),
            new BasicDBObject("b", 2)
        )),
        new BasicDBObject("a", asList(
            new BasicDBObject("b", 2),
            new BasicDBObject("b", 3)
        ))
    );
    assertEquals(Arrays.<DBObject>asList(
        new BasicDBObject("a", asList(
            new BasicDBObject("b", 2),
            new BasicDBObject("b", 3)
        ))
    ), results);
  }

  @Test
  public void testNeOperator() {
    DBObject query = new BasicDBObjectBuilder().push("a").add("$ne", 3).pop().get();
    List<DBObject> results = doFilter(
        query,
        new BasicDBObject("a", asList(1, 3)),
        new BasicDBObject("a", 1),
        new BasicDBObject("a", 3),
        new BasicDBObject("b", 3),
        new BasicDBObject("a", asList(1, 2))
    );
    assertEquals(Arrays.<DBObject>asList(
        new BasicDBObject("a", 1),
        new BasicDBObject("b", 3),
        new BasicDBObject("a", asList(1, 2))
    ), results);
  }

  @Test
  public void testNeOperatorWithNullValue() {
	  DBObject query = new BasicDBObjectBuilder().push("a").add("$ne", null).pop().get();
	    List<DBObject> results = doFilter(
	        query,
	        new BasicDBObject("a", asList(null, 3)),
	        new BasicDBObject("a", null),
	        new BasicDBObject("a", 1),
	        new BasicDBObject("b", 3),
	        new BasicDBObject("a", asList(1, 2))
	    );
	    assertEquals(Arrays.<DBObject>asList(
	        new BasicDBObject("a", 1),
	        new BasicDBObject("a", asList(1, 2))
	    ), results);
  }

  @Test
  public void testNeOperatorWithNullValueSubObject() {
	  DBObject query = new BasicDBObjectBuilder().push("a.b").add("$ne", null).pop().get();
	    List<DBObject> results = doFilter(
	        query,
	        new BasicDBObject("a", new BasicDBObject("b", null)),
	        new BasicDBObject("a", new BasicDBObject("b", 1)),
	        new BasicDBObject("a", new BasicDBObject("c", null)),
	        new BasicDBObject("a", new BasicDBObject("b",  new BasicDBObject("c", 1))),
	        new BasicDBObject("a", null),
	        new BasicDBObject("a", 1),
	        new BasicDBObject("b", 1)
	    );
	    assertEquals(Arrays.<DBObject>asList(
	        new BasicDBObject("a", new BasicDBObject("b", 1)),
	        new BasicDBObject("a", new BasicDBObject("b",  new BasicDBObject("c", 1)))
	    ), results);
  }
  @Test
  public void testNeOperatorWithEmptyList() {
      BasicDBList listOfSingleItem = new BasicDBList();
      listOfSingleItem.add(1);

	  DBObject query = new BasicDBObjectBuilder().push("list").add("$ne", new BasicDBList()).pop().get();
	  List<DBObject> results = doFilter(
	        query,
	        new BasicDBObject("list", new BasicDBList()),
	        new BasicDBObject("list", listOfSingleItem)
	  );
	  assertEquals(Arrays.<DBObject>asList(
              new BasicDBObject("list", listOfSingleItem)
	  ), results);
  }

  @Test
  public void testNeOperatorWithNonEmptyList() {
      BasicDBList listOfSingleItem = new BasicDBList();
      listOfSingleItem.add(1);

	  DBObject query = new BasicDBObjectBuilder().push("list").add("$ne", listOfSingleItem).pop().get();
	  List<DBObject> results = doFilter(
	        query,
	        new BasicDBObject("list", new BasicDBList()),
	        new BasicDBObject("list", listOfSingleItem)
	  );
	  assertEquals(Arrays.<DBObject>asList(
              new BasicDBObject("list", new BasicDBList())
	  ), results);
  }

  @Test
  public void testNeEmbeddedOperator() {

    DBObject query = new BasicDBObject("a.b", new BasicDBObject("$ne", 2));
    List<DBObject> results = doFilter(
        query,
        new BasicDBObject("a", asList(
            new BasicDBObject("b", 1),
            new BasicDBObject("b", 2)
        )),
        new BasicDBObject("a", asList(
            new BasicDBObject("b", 3),
            new BasicDBObject("b", 4)
        ))
    );
    assertEquals(Arrays.<DBObject>asList(
        new BasicDBObject("a", asList(
            new BasicDBObject("b", 3),
            new BasicDBObject("b", 4)
        ))
    ), results);
  }

  @Test
  public void testAllOperator() {
    DBObject query = new BasicDBObjectBuilder().push("a").add("$all", asList(2, 3)).pop().get();
    List<DBObject> results = doFilter(
        query,
        new BasicDBObject("a", asList(2, 3)),
        new BasicDBObject("a", null),
        new BasicDBObject("a", asList(1, 3, 4)),
        new BasicDBObject("a", asList(1, 2, 3)),
        new BasicDBObject("a", asList(1, 3, 4))
    );
    assertEquals(Arrays.<DBObject>asList(
        new BasicDBObject("a", asList(2, 3)),
        new BasicDBObject("a", asList(1, 2, 3))
    ), results);
  }

  @Test
  public void testExistsOperator() {
    DBObject query = new BasicDBObjectBuilder().push("a").add("$exists", true).pop().get();
    List<DBObject> results = doFilter(
        query,
        new BasicDBObject("a", null),
        new BasicDBObject("b", null),
        new BasicDBObject("a", "hi"),
        new BasicDBObject("b", "hi")
    );
    assertEquals(Arrays.<DBObject>asList(
        new BasicDBObject("a", null),
        new BasicDBObject("a", "hi")
    ), results);
  }

  @Test
  public void testModOperator() {
    DBObject query = new BasicDBObjectBuilder().push("a").add("$mod", asList(10, 1)).pop().get();
    List<DBObject> results = doFilter(
        query,
        new BasicDBObject("a", 1),
        new BasicDBObject("a", null),
        new BasicDBObject("a", 21),
        new BasicDBObject("a", 22)
    );
    assertEquals(Arrays.<DBObject>asList(
        new BasicDBObject("a", 1),
        new BasicDBObject("a", 21)
    ), results);
  }

  @Test
  public void testInOperator() {
    DBObject query = new BasicDBObjectBuilder().push("a").add("$in", asList(2, 3)).pop().get();
    List<DBObject> results = doFilter(
        query,
        new BasicDBObject("a", asList(1, 3)),
        new BasicDBObject("a", 1),
        new BasicDBObject("a", 3)
    );
    assertEquals(Arrays.<DBObject>asList(
        new BasicDBObject("a", asList(1, 3)),
        new BasicDBObject("a", 3)
    ), results);
  }

  @Test
  public void testInOperatorWithNullValue() {
    // Querying for null should return positive if the field is absent.
    // See: http://docs.mongodb.org/manual/faq/developers/#faq-developers-query-for-nulls
    DBObject query = new BasicDBObjectBuilder().push("a").add("$in", asList(2, 3, null)).pop().get();
    List<DBObject> results = doFilter(
        query,
        new BasicDBObject("a", asList(1, 3)),
        new BasicDBObject("a", 1),
        new BasicDBObject("a", 3),
        new BasicDBObject("b", 1)
    );
    assertEquals(Arrays.<DBObject>asList(
        new BasicDBObject("a", asList(1, 3)),
        new BasicDBObject("a", 3),
        new BasicDBObject("b", 1)
    ), results);
  }

  @Test
  public void testInEmbeddedOperator() {
    DBObject query = new BasicDBObject("a.b", new BasicDBObject("$in", asList(2)));
    List<DBObject> results = doFilter(
        query,
        new BasicDBObject("a", asList(
            new BasicDBObject("b", 1),
            new BasicDBObject("b", 2)
        )),
        new BasicDBObject("a", asList(
            new BasicDBObject("b", 3),
            new BasicDBObject("b", 4)
        ))
    );
    assertEquals(Arrays.<DBObject>asList(
        new BasicDBObject("a", asList(
            new BasicDBObject("b", 1),
            new BasicDBObject("b", 2)
        ))
    ), results);
  }
  
  @SuppressWarnings("unchecked")
  @Test
  public void testInListOfLists() {
    DBObject query = new BasicDBObject("a", new BasicDBObject("$in", asList(asList(1, 2))));
    List<DBObject> results = doFilter(
        query,
        new BasicDBObject("a", asList(asList(0))),
        new BasicDBObject("a", asList(asList(1))),
        new BasicDBObject("a", asList(asList(2))),
        new BasicDBObject("a", asList(asList(1), asList(2))),
        new BasicDBObject("a", asList(1, 2))
    );
    assertEquals(Arrays.<DBObject>asList(
        new BasicDBObject("a", asList(1, 2))
    ), results);
  }

  @Test
  public void testNinOperator() {
    DBObject query = new BasicDBObjectBuilder().push("a").add("$nin", asList(2, 3)).pop().get();
    List<DBObject> results = doFilter(
        query,
        new BasicDBObject("a", asList(1, 4)),
        new BasicDBObject("a", asList(1, 3)),
        new BasicDBObject("a", 1),
        new BasicDBObject("a", 3)
    );
    assertEquals(Arrays.<DBObject>asList(
        new BasicDBObject("a", asList(1, 4)),
        new BasicDBObject("a", 1)
    ), results);
  }

  @Test
  public void testNinEmbeddedOperator() {

    DBObject query = new BasicDBObject("a.b", new BasicDBObject("$nin", asList(2)));
    List<DBObject> results = doFilter(
        query,
        new BasicDBObject("a", asList(
            new BasicDBObject("b", 1),
            new BasicDBObject("b", 2)
        )),
        new BasicDBObject("a", asList(
            new BasicDBObject("b", 3),
            new BasicDBObject("b", 4)
        ))
    );
    assertEquals(Arrays.<DBObject>asList(
        new BasicDBObject("a", asList(
            new BasicDBObject("b", 3),
            new BasicDBObject("b", 4)
        ))
    ), results);
  }

  @Test
  public void testNinMissingOperator() {
    DBObject query = new BasicDBObjectBuilder().push("a").add("$nin", asList(2, 3)).pop().get();
    List<DBObject> results = doFilter(
        query,
        new BasicDBObject("a", asList(1, 4)),
        new BasicDBObject("a", asList(1, 3)),
        new BasicDBObject("a", 1),
        new BasicDBObject("a", 3),
        new BasicDBObject("b", 3)
    );
    assertEquals(Arrays.<DBObject>asList(
        new BasicDBObject("a", asList(1, 4)),
        new BasicDBObject("a", 1),
        new BasicDBObject("b", 3)
    ), results);
  }

  @Test
  public void testNotComplexOperator() {
    DBObject query = new BasicDBObjectBuilder().push("a")
        .push("$not").add("$nin", asList(2, 3)).pop().pop().get();
    List<DBObject> results = doFilter(
        query,
        new BasicDBObject("a", asList(1, 4)),
        new BasicDBObject("a", asList(1, 3)),
        new BasicDBObject("a", 1),
        new BasicDBObject("a", 3)
    );
    assertEquals(Arrays.<DBObject>asList(
        new BasicDBObject("a", asList(1, 3)),
        new BasicDBObject("a", 3)
    ), results);


  }

  @Test
  public void testNotSimpleOperator() {
    DBObject query = new BasicDBObjectBuilder().push("a").add("$not", 3).pop().get();
    List<DBObject> results = doFilter(
        query,
        new BasicDBObject("a", asList(1, 4)),
        new BasicDBObject("a", asList(1, 3)),
        new BasicDBObject("a", 1),
        new BasicDBObject("a", 3)
    );
    assertEquals(Arrays.<DBObject>asList(
        new BasicDBObject("a", asList(1, 4)),
        new BasicDBObject("a", 1)
    ), results);
  }

  @Test
  public void testEmbeddedMatch() {
    DBObject query = new BasicDBObject("a.b", 1);
    List<DBObject> results = doFilter(
        query,
        new BasicDBObject("a", 1),
        new BasicDBObject("b", 1),
        new BasicDBObject("a", new BasicDBObject("b", 1))
    );
    assertEquals(Arrays.<DBObject>asList(
        new BasicDBObject("a", new BasicDBObject("b", 1))
    ), results);
  }

  @Test
  public void testEmbeddedArrayMatch() {
    DBObject query = new BasicDBObject("a.0.b", 1);
    List<DBObject> results = doFilter(
        query,
        new BasicDBObject("a", asList(new BasicDBObject("b", 2))),
        new BasicDBObject("b", 1),
        new BasicDBObject("a", asList(new BasicDBObject("b", 1)))
    );
    assertEquals(Arrays.<DBObject>asList(
        new BasicDBObject("a", asList(new BasicDBObject("b", 1)))
    ), results);
  }

  @Test
  public void testEmbeddedArrayObjectMatch() {
    DBObject query = new BasicDBObject("a.b.c", 1);
    List<DBObject> results = doFilter(
        query,
        new BasicDBObject("a", asList(new BasicDBObject("b", new BasicDBObject("c", 1)))),
        new BasicDBObject("a", asList(new BasicDBObject("b", 1)))
    );
    assertEquals(Arrays.<DBObject>asList(
        new BasicDBObject("a", asList(new BasicDBObject("b", new BasicDBObject("c", 1))))
    ), results);
  }

  @Test
  public void testEmbeddedArrayObjectMultiMatch() {
    DBObject query = new BasicDBObject("a.b", 1).append("a.c", 1);
    List<DBObject> results = doFilter(
        query,
        new BasicDBObject("a", asList(new BasicDBObject("b", 1).append("c", 1)))
    );
    assertEquals(Arrays.<DBObject>asList(
        new BasicDBObject("a", asList(new BasicDBObject("b", 1).append("c", 1)))
    ), results);
  }

  @Test
  public void testEmptyDocumentQueryValue() {
    DBObject query = new BasicDBObject("a", new BasicDBObject());
    List<DBObject> results = doFilter(
      query,
      new BasicDBObject("a", new BasicDBObject("b", 1))
    );
    assertTrue(results.isEmpty());
  }

  @Test
  public void testEmbeddedEmptyMatch() {
    DBObject query = new BasicDBObject("a.b.c", 1);
    List<DBObject> results = doFilter(
        query,
        new BasicDBObject("a", asList(new BasicDBObject("b", new BasicDBObject("c", 1)))),
        new BasicDBObject("a", asList()),
        new BasicDBObject("a", asDbList(new BasicDBObject("b", new BasicDBObject("c", 1)))),
        new BasicDBObject("a", asDbList())
    );
    assertEquals(Arrays.<DBObject>asList(
        new BasicDBObject("a", asList(new BasicDBObject("b", new BasicDBObject("c", 1)))),
        new BasicDBObject("a", asDbList(new BasicDBObject("b", new BasicDBObject("c", 1))))
    ), results);
  }

  @Test
  public void testOrOperator() {
    DBObject query = new BasicDBObject(new String("$or"), asList(
        new BasicDBObject("a", 3),
        new BasicDBObject("b", new BasicDBObject("$ne", 3))
    ));
    List<DBObject> results = doFilter(
        query,
        new BasicDBObject("a", 3).append("b", 1),
        new BasicDBObject("a", 1).append("b", 3),
        new BasicDBObject("a", 1).append("b", 1),
        new BasicDBObject("a", 3),
        new BasicDBObject("b", 1),
        new BasicDBObject("a", 5),
        new BasicDBObject("b", 3)
    );
    assertEquals(Arrays.<DBObject>asList(
        new BasicDBObject("a", 3).append("b", 1),
        new BasicDBObject("a", 1).append("b", 1),
        new BasicDBObject("a", 3),
        new BasicDBObject("b", 1),
        new BasicDBObject("a", 5) //i wasn't expected this result, but it works same way in mongo
    ), results);
  }

  @Test
  public void testComplexOrOperator() {
    DBObject query = new BasicDBObject("$or", asList(
        new BasicDBObject("a", 3),
        new BasicDBObject("$or", asList(
            new BasicDBObject("b", 1),
            new BasicDBObject("b", 3)
        ))
    ));
    List<DBObject> results = doFilter(
        query,
        new BasicDBObject("a", 3).append("b", 1),
        new BasicDBObject("a", 1).append("b", 3),
        new BasicDBObject("a", 1).append("b", 7),
        new BasicDBObject("a", 3),
        new BasicDBObject("b", 1),
        new BasicDBObject("a", 5),
        new BasicDBObject("b", 7)
    );
    assertEquals(Arrays.<DBObject>asList(
        new BasicDBObject("a", 3).append("b", 1),
        new BasicDBObject("a", 1).append("b", 3),
        new BasicDBObject("a", 3),
        new BasicDBObject("b", 1)
    ), results);
  }

  @Test
  public void testRegexPattern() {
    DBObject query = new BasicDBObject("a", Pattern.compile("^foo"));
    List<DBObject> results = doFilter(
        query,
        new BasicDBObject("a", 1),
        new BasicDBObject("a", null),
        new BasicDBObject("a", "fooSter"),
        new BasicDBObject("a", "funky foo"),
        new BasicDBObject("a", asList("foomania", "notfoo"))
    );
    assertEquals(Arrays.<DBObject>asList(
        new BasicDBObject("a", "fooSter"),
        new BasicDBObject("a", asList("foomania", "notfoo"))
    ), results);
  }

  @Test
  public void testRegexOperator() {
    DBObject query = new BasicDBObject("a", new BasicDBObject("$regex", "^foo"));

    List<DBObject> results = doFilter(
        query,
        new BasicDBObject("a", "fooSter")
    );
    assertEquals(Arrays.<DBObject>asList(
        new BasicDBObject("a", "fooSter")
    ), results);
  }

  @Test
  public void parseRegexFlags() {
    ExpressionParser ep = new ExpressionParser();
    assertEquals(Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.COMMENTS, ep.parseRegexOptionsToPatternFlags("ixs"));
  }

  @Test
  public void testRegexEmbeddedOperator() {
    DBObject query = new BasicDBObject("a.b", Pattern.compile("^foo"));
    List<DBObject> results = doFilter(
        query,
        new BasicDBObject("a", asList(
            new BasicDBObject("b", "bar"),
            new BasicDBObject("b", "fooBar")
        ))

    );
    assertEquals(Arrays.<DBObject>asList(
        new BasicDBObject("a", asList(
            new BasicDBObject("b", "bar"),
            new BasicDBObject("b", "fooBar")
        ))
    ), results);
  }

  @Test
  public void testRegexOperatorWithMultilineDoc() {
    BasicDBObject regexPattern = new BasicDBObject(ExpressionParser.REGEX, "foo.*Ster")
        .append(ExpressionParser.REGEX_OPTIONS, "s");
    DBObject query = new BasicDBObject("a", regexPattern);

    List<DBObject> results = doFilter(
        query,
        new BasicDBObject("a", "foo\nSter")
    );
    assertEquals(Arrays.<DBObject>asList(
        new BasicDBObject("a", "foo\nSter")
    ), results);
  }

  @Test
  public void testConditionalWithDate() {
    DBObject query = new BasicDBObjectBuilder().push("a").add("$lte", new Date(2)).pop().get();
    List<DBObject> results = doFilter(
        query,
        new BasicDBObject("a", null),
        new BasicDBObject("a", new Date(2)),
        new BasicDBObject("a", new Date(1)),
        new BasicDBObject("a", new Date(3))
    );
    assertEquals(Arrays.<DBObject>asList(
        new BasicDBObject("a", new Date(2)),
        new BasicDBObject("a", new Date(1))
    ), results);
  }

  @Test
  public void testCompoundDateRange() {
    DBObject query = new BasicDBObjectBuilder().push("_id")
        .push("$lt").add("n", "a").add("t", new Date(10)).pop()
        .push("$gte").add("n", "a").add("t", new Date(1)).pop()
        .pop().get();
    List<DBObject> results = doFilter(
        query,
        new BasicDBObject("_id", new BasicDBObject("n", "a").append("t", new Date(1))),
        new BasicDBObject("_id", new BasicDBObject("n", "a").append("t", new Date(2))),
        new BasicDBObject("_id", new BasicDBObject("n", "a").append("t", new Date(3))),
        new BasicDBObject("_id", new BasicDBObject("n", "a").append("t", new Date(11)))
    );
    assertEquals(Arrays.<DBObject>asList(
        new BasicDBObject("_id", new BasicDBObject("n", "a").append("t", new Date(1))),
        new BasicDBObject("_id", new BasicDBObject("n", "a").append("t", new Date(2))),
        new BasicDBObject("_id", new BasicDBObject("n", "a").append("t", new Date(3)))
    ), results);
  }

  @Test
  public void testSizeOperator() {
    DBObject query = new BasicDBObjectBuilder().push("a").add("$size", 3).pop().get();
    List<DBObject> results = doFilter(
        query,
        new BasicDBObject("a", null),
        new BasicDBObject("a", asList(1, 2, 3)),
        new BasicDBObject("a", asList(1, 2, 3, 4))
    );
    assertEquals(Arrays.<DBObject>asList(
        new BasicDBObject("a", asList(1, 2, 3))
    ), results);
  }

  @Test
  public void testCompoundObjectInQuery() {
    ObjectId oid = new ObjectId();
    DBObject query = new BasicDBObject("a", new BasicDBObject("b", oid));
    List<DBObject> results = doFilter(
        query,
        new BasicDBObject("a", null),
        new BasicDBObject("a", new BasicDBObject("b", oid))
    );
    assertEquals(Arrays.<DBObject>asList(
        new BasicDBObject("a", new BasicDBObject("b", oid))
    ), results);
  }

  @Test
  public void testCompareObjects() {
    ExpressionParser expressionParser = new ExpressionParser();
    assertEquals(0, expressionParser.compareObjects(new BasicDBObject(), new BasicDBObject()));
    assertTrue(0 < expressionParser.compareObjects(new BasicDBObject("a", 3), new BasicDBObject("a", 1)));
    assertTrue(0 < expressionParser.compareObjects(new BasicDBObject("a", asList(2, 3)), new BasicDBObject("a", asList(1, 2))));
    assertTrue(0 < expressionParser.compareObjects(new BasicDBList(), new BasicDBObject()));
  }

  @Test
  public void compare_dbref() {
    Fongo fongo = new Fongo("test");
    ExpressionParser expressionParser = new ExpressionParser();
    DBRef first = new DBRef(fongo.getDB("test"), "coll", ObjectId.get());
    DBRef second = new DBRef(fongo.getDB("test"), "coll", ObjectId.get());
    DBRef third = new DBRef(fongo.getDB("test"), "coll2", first.getId());
    assertThat(expressionParser.compareObjects(first, first)).isEqualTo(0);
    assertThat(expressionParser.compareObjects(first, second)).isNotEqualTo(0);
    assertThat(expressionParser.compareObjects(first, third)).isNotEqualTo(0);
  }

  @Test
  public void testCompareObjectsMinMax() {
    ExpressionParser expressionParser = new ExpressionParser();
    assertTrue(0 > expressionParser.compareObjects(new MinKey(), null));
    assertTrue(0 < expressionParser.compareObjects(new MaxKey(), null));
  }

  @Test
  public void testCompareLists() {
    ExpressionParser expressionParser = new ExpressionParser();
    assertTrue(0 > expressionParser.compareObjects(Arrays.asList(new MinKey()), Arrays.asList()));
    assertTrue(0 < expressionParser.compareObjects(Arrays.asList(new MaxKey()), Arrays.asList()));
    assertTrue(0 < expressionParser.compareObjects(Arrays.asList(), Arrays.asList(new MinKey())));
    assertTrue(0 > expressionParser.compareObjects(Arrays.asList(), Arrays.asList(new MaxKey())));
  }

  @Test
  public void testCompareDBObjects() {
    ExpressionParser expressionParser = new ExpressionParser();

    DBObject obja0 = new BasicDBObjectBuilder().append("a", 0).get();
    DBObject obja1b2 = new BasicDBObjectBuilder().append("a", 1).append("b", 2).get();
    DBObject obja2 = new BasicDBObjectBuilder().append("a", 2).get();
    DBObject objb0 = new BasicDBObjectBuilder().append("b", 0).get();
    DBObject objb2a1 = new BasicDBObjectBuilder().append("b", 2).append("a", 1).get();

    assertTrue(0 > expressionParser.compareObjects(obja0, obja1b2));
    assertTrue(0 > expressionParser.compareObjects(obja1b2, obja2));
    assertTrue(0 > expressionParser.compareObjects(obja2, objb0));
    assertTrue(0 > expressionParser.compareObjects(objb0, objb2a1));

    assertTrue(0 < expressionParser.compareObjects(obja1b2, obja0));
    assertTrue(0 < expressionParser.compareObjects(obja2, obja1b2));
    assertTrue(0 < expressionParser.compareObjects(objb0, obja2));
    assertTrue(0 < expressionParser.compareObjects(objb2a1, objb0));
  }

  @Test
  public void testCompareTo() {
    ExpressionParser expressionParser = new ExpressionParser();
    assertTrue(0 < expressionParser.compareTo(Boolean.TRUE, 1));
    assertTrue(0 > expressionParser.compareTo(1, Boolean.TRUE));
    assertTrue(0 > expressionParser.compareTo(1, 2.0));
    assertTrue(0 < expressionParser.compareTo(2.0, 1));
    assertTrue(0 > expressionParser.compareTo(100, new Date()));
    assertTrue(0 < expressionParser.compareTo(new Date(), 100));
    assertTrue(0 < expressionParser.compareTo(new MaxKey(), new MinKey()));
    assertTrue(0 > expressionParser.compareTo(new MinKey(), new MaxKey()));
    assertTrue(0 == expressionParser.compareTo(new MinKey(), new MinKey()));
    assertTrue(0 == expressionParser.compareTo(new MaxKey(), new MaxKey()));
    assertTrue(0 > expressionParser.compareTo(12, new MaxKey()));
    assertTrue(0 < expressionParser.compareTo(new MaxKey(), 12));
    assertTrue(0 < expressionParser.compareTo(12, new MinKey()));
    assertTrue(0 > expressionParser.compareTo(new MinKey(), 12));
    // No difference between Long and Integer
    assertTrue(0 == expressionParser.compareTo(3L, 3));
    assertTrue(0 == expressionParser.compareTo(3, 3L));
    // No difference between Double and Float
    assertTrue(0 == expressionParser.compareTo(3D, 3F));
    assertTrue(0 == expressionParser.compareTo(3F, 3D));
    // False before true
    assertTrue(0 > expressionParser.compareTo(false, true));
    assertTrue(0 < expressionParser.compareTo(true, false));

    assertTrue(0 == expressionParser.compareTo(null, null));
    assertTrue(0 < expressionParser.compareTo(12, null));
    assertTrue(0 > expressionParser.compareTo(null, 12));

    assertTrue(0 < expressionParser.compareTo(new BasicDBObject("a", 3), "3"));
    assertTrue(0 < expressionParser.compareTo("3", 3));
    assertTrue(0 < expressionParser.compareTo(ObjectId.get(), new BasicDBObject("a", 3)));
    assertTrue(0 > expressionParser.compareTo(ObjectId.get(), Pattern.compile("a*")));
    assertTrue(0 > expressionParser.compareTo(new Date(), Pattern.compile("a*")));

    assertTrue(0 < expressionParser.compareTo(new BasicDBList(), new BasicDBObject("a", 3)));

    // For NOT converting Double to Long
    assertEquals(1, expressionParser.compareTo(-9223372036854775808L, (double) -9223372036854775807L));
    assertEquals(-1, expressionParser.compareTo((double) -9223372036854775807L, -9223372036854775808L));
  }

  @Test
  public void testCompareToBinary() {
    ExpressionParser expressionParser = new ExpressionParser();

    // Binary must be handled
    assertTrue(0 == expressionParser.compareTo(new Binary("jon".getBytes()), new Binary("jon".getBytes())));
    assertTrue(0 > expressionParser.compareTo(new Binary("JON".getBytes()), new Binary("jon".getBytes())));
    assertTrue(0 < expressionParser.compareTo(new Binary("jon".getBytes()), new Binary("JON".getBytes())));
  }

  @Test
  public void testCompareToBytesArray() {
    ExpressionParser expressionParser = new ExpressionParser();

    // Binary must be handled
    assertTrue(0 == expressionParser.compareTo("jon".getBytes(), "jon".getBytes()));
    assertTrue(0 > expressionParser.compareTo("JON".getBytes(), "jon".getBytes()));
    assertTrue(0 < expressionParser.compareTo("jon".getBytes(), "JON".getBytes()));
  }

  @Test
  public void testSortComparatorOnMixedArray() {
    ExpressionParser expressionParser = new ExpressionParser();

    Date date = new Date();
    DBObject obja0 = new BasicDBObjectBuilder().append("a", 0).get();
    DBObject obja2 = new BasicDBObjectBuilder().append("a", 2).get();
    DBObject obja1b2 = new BasicDBObjectBuilder().append("a", 1).append("b", 2).get();
    DBObject objb0 = new BasicDBObjectBuilder().append("b", 0).get();
    List<Object> list = new BasicDBList();
    double d = 0.5D;
    int i = 0;
    MinKey minKey = new MinKey();
    MaxKey maxKey = new MaxKey();
    long l = 1L;
    ObjectId objId = new ObjectId();
    Pattern regex = Pattern.compile("\\s*");

    // arbitrary order
    List<Object> objects = Util.list(obja0, obja2, objb0, obja1b2, d, i, null, minKey, maxKey, l, false, true, date, regex, objId, list, "");

    Collections.sort(objects, expressionParser.sortSpecificationComparator(new BasicDBObjectBuilder().append("a", 1).get()));
    assertArrayEquals(
        new Object[]{minKey, null, i, d, l, "", objb0, list, objId, false, true, date, regex, maxKey, obja0, obja1b2, obja2},
        objects.toArray()
    );

    Collections.sort(objects, expressionParser.sortSpecificationComparator(new BasicDBObjectBuilder().append("a", -1).get()));
    assertArrayEquals(
        new Object[]{obja2, obja1b2, obja0, minKey, null, i, d, l, "", objb0, list, objId, false, true, date, regex, maxKey},
        objects.toArray()
    );
  }

  @Test
  public void testItemInList() {
    DBObject query = BasicDBObjectBuilder.start()
        .push("_id").append("$in", asList(new ObjectId("4f39d7904b90b2f2f1530849"), new ObjectId("4f39d78d4b90b2f2f1530841"))).pop()
        .push("c").append("$in", asList(new ObjectId("4f39d78d4b90b2f2f153083b"))).pop().get();

    BasicDBObject expectedResult = new BasicDBObject("_id", new ObjectId("4f39d78d4b90b2f2f1530841")).append("c", asList(new ObjectId("4f39d78d4b90b2f2f153083b")));
    List<DBObject> results = doFilter(
        query,
        expectedResult
    );
    assertEquals(Arrays.<DBObject>asList(
        expectedResult
    ), results);
  }

  @Test
  public void testComplexBounds() {
    DBObject query = new BasicDBObjectBuilder().push("_id")
        .append("$gte", new BasicDBObject("u", 1).append("v", new ObjectId("000000000000000000000000")))
        .append("$lte", new BasicDBObject("u", 2).append("v", new ObjectId("000000000000000000000000")))
        .push("c").append("$gt", 0).pop().pop().get();

    BasicDBObject rec1 = new BasicDBObject("_id", new BasicDBObject("u", 1).append("v", new ObjectId())).append("c", 1);
    BasicDBObject rec2 = new BasicDBObject("_id", new BasicDBObject("u", 1).append("v", new ObjectId())).append("c", 1);
    List<DBObject> results = doFilter(
        query,
        rec1,
        rec2
    );

    assertEquals(Arrays.<DBObject>asList(
        rec1,
        rec2
    ), results);
  }

  @Test
  public void testOperatorInMap() throws Exception {
    HashMap<String, Object> operatorMap = new HashMap<String, Object>();
    operatorMap.put("$in", asList(1, 2, 3));
    BasicDBObject query = new BasicDBObject("a", operatorMap);
    BasicDBObject rec1 = new BasicDBObject("_id", 1).append("a", 1).append("b", "5").append("c", 1);
    BasicDBObject rec2 = new BasicDBObject("_id", 2).append("a", 4).append("b", "8").append("c", 1);
    List<DBObject> results = doFilter(
        query,
        rec1,
        rec2
    );
    assertEquals(Arrays.<DBObject>asList(rec1), results);
  }

  @Test
  public void testDBRef() throws Exception {
    BasicDBObject rec1 = new BasicDBObject("a", new DBRef(null, "c", 1));
    List<DBObject> results = doFilter(
        new BasicDBObject("a.$id", 1),
        rec1,
        new BasicDBObject("a", new DBRef(null, "c", 2)));
    assertEquals(Arrays.<DBObject>asList(rec1), results);
  }

  @Test
  public void testDBRefList() throws Exception {
    BasicDBObject rec1 = new BasicDBObject("a", asList(new DBRef(null, "c", 1), new DBRef(null, "c", 2)));
    List<DBObject> results = doFilter(
        new BasicDBObject("a.$id", 1),
        rec1,
        new BasicDBObject("a", asList(new DBRef(null, "c", 2))));

    assertEquals(Arrays.<DBObject>asList(rec1), results);
  }

  @Test
  public void testListsMatch() {
    DBObject query = new BasicDBObject("a", asList(1, 2, 3));
    List<DBObject> results = doFilter(
        query,
        new BasicDBObject("a", asList()),
        new BasicDBObject("b", asList()),
        new BasicDBObject("a", asList(1, 2, 3)),
        new BasicDBObject("a", asDbList(1, 2, 3)),
        new BasicDBObject("a", asDbList())
    );
    assertEquals(Arrays.<DBObject>asList(
        new BasicDBObject("a", asList(1, 2, 3)),
        new BasicDBObject("a", asDbList(1, 2, 3))
    ), results);
  }

  @Test
  public void testWhereExpression() {
    assertQuery(new BasicDBObject("$where", "this.n == 'fred'"), Arrays.<DBObject>asList(
        new BasicDBObject("n", "fred").append("a", 2)
    ));
    assertQuery(new BasicDBObject("$where", "this.a <= 3"), Arrays.<DBObject>asList(
        new BasicDBObject("a", null),
        new BasicDBObject("n", "neil").append("a", 1),
        new BasicDBObject("n", "fred").append("a", 2),
        new BasicDBObject("n", "ted").append("a", 3)
    ));
  }

  private void assertQuery(BasicDBObject query, List<DBObject> expected) {
    List<DBObject> results = doFilter(
        query,
        new BasicDBObject("a", null),
        new BasicDBObject("n", "neil").append("a", 1),
        new BasicDBObject("n", "fred").append("a", 2),
        new BasicDBObject("n", "ted").append("a", 3),
        new BasicDBObject("n", "stu").append("a", 4),
        new BasicDBObject("n", "tim").append("a", 5),
        new BasicDBObject("a", asList(3, 4))
    );
    assertEquals(expected, results);
  }

  public List<DBObject> doFilter(DBObject ref, DBObject... input) {
    ExpressionParser ep = new ExpressionParser();
    Filter filter = ep.buildFilter(ref);
    List<DBObject> results = new ArrayList<DBObject>();
    for (DBObject dbo : input) {
      if (filter.apply(dbo)) {
        results.add(dbo);
      }
    }
    return results;
  }

  <T> List<T> asList(T... ts) {
    return Arrays.asList(ts);
  }

  BasicDBList asDbList(Object... objects) {
    BasicDBList list = new BasicDBList();
    list.addAll(Arrays.asList(objects));
    return list;
  }

}
