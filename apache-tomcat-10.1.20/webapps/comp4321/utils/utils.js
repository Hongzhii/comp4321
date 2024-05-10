function showResults(response) {
  // Clear the existing content of the data container
  $("#dataContainer").empty();
  $("#num-results").empty();
  $("#searchResults").empty();
  console.log("show results");
  console.log("response", response);
  // Parse the response data
  // response = response.replace('"', '\"')
  // response = JSON.stringify(response);
  response = JSON.parse(response);
  console.log(typeof response);
  console.log("response:", response);
  // response = JSON.parse(response); // not a duplication this is required

  // Extract the search input and search results
  // response = {
  //   input: response.input,
  //   results: [
  //     {
  //       title: "title1",
  //       url: "url1",
  //       lastModified: "lastModified1",
  //       size: "size1",
  //       keywords: ["keyword1", "keyword2", "keyword3"],
  //       children: ["child1", "child2", "child3"],
  //       score: "0.5",
  //     },
  //     {
  //       title: "title1",
  //       url: "url1",
  //       lastModified: "lastModified1",
  //       size: "size1",
  //       keywords: ["keyword1", "keyword2", "keyword3"],
  //       children: ["child1", "child2", "child3"],
  //       score: "0.6",
  //     },
  //     {
  //       title: "title1",
  //       url: "url1",
  //       lastModified: "lastModified1",
  //       size: "size1",
  //       keywords: [["keyword1", freq1], "keyword2", "keyword3"],
  //       children: ["child1", "child2", "child3"],
  //       score: "0.7",
  //     },
  //   ],
  // };

  //test section
  var size = response.results.length;
  console.log(size);
  for (var i = 0; i < size; i++) {
    var container = $(`<div></div>`);
    var title = response.results[i].title;
    var url = response.results[i].url;
    var lastModifiedDate = response.results[i].lastModified;
    var sizeofPage = response.results[i].size;
    var entrycontainer = $(`<div>${title}</div>`);
    var pageUrl = $(`<div><a href=${url}>${url}</a></div>`);
    var lastModnSize = $(
      `<div> Last Modified: ${lastModifiedDate}, Size of Page: ${sizeofPage}kb </div>`
    );
    entrycontainer.append(lastModnSize);
    entrycontainer.append([pageUrl]);
    var keywords = $(`<div>`);
    var keywordArray = response.results[i].wordFrequencies;
    var keywordCount = keywordArray.length;
    for (var j = 0; j < keywordCount; j++) {
      keywords.append(keywordArray[j].word + ": " + keywordArray[j].frequency);
      if (j != keywordCount - 1) {
        keywords.append(";");
      }
    }
    keywords.append(`</div>`);
    entrycontainer.append(keywords);
    var childlinks = response.results[i].childLinks.length;
    var childUrl = response.results[i].childLinks;
    for (var j = 0; j < childlinks; j++) {
      var childlink = $(`<div>${childUrl[j]}</div>`);
      entrycontainer.append(childlink);
    }
    var score = response.results[i].score;
    var scorecontainer = $(`<div> score: ${score}</div>`);
    entrycontainer.append(scorecontainer);
    container.append(entrycontainer);
    $("#searchResults").append(container);
  }
}

// Attach event handler for "show more" button
$(document).on("click", ".show-more-btn", function () {
  // Toggle visibility of extra links
  $(this).siblings(".child-link:nth-child(n+6)").toggle();

  // Update button text based on visibility
  $(this).text($(this).text() === "Show More" ? "Show Less" : "Show More");
});
