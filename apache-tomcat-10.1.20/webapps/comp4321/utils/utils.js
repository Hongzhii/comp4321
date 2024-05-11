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
  //console.log(size);
  for (var i = 0; i < size; i++) {
    var container = $(`<div class="entry"></div>`); //container for each entry
    var title = response.results[i].title;
    var score = response.results[i].score;

    //Title and score displayed by default 
    var header = $(`<div class="header">Title: ${title} | Score: ${score}</div>`);
    container.append(header);


    // Rest of the details initially hidden
    var detailsContainer = $('<div class="details" style="display: none;"></div>');
    

    var url = response.results[i].url;
    var lastModifiedDate = response.results[i].lastModified;
    var sizeofPage = response.results[i].size;
    //var entrycontainer = $(`<div>Title:${title}</div>`);
    // var pageUrl = $(`<div>URL:<a href=${url}>${url}</a></div>`);
    // var lastModnSize = $(
    //   `<div> Last Modified: ${lastModifiedDate}, Size of Page: ${sizeofPage}kb </div>`
    // );
    // entrycontainer.append(lastModnSize);
    // entrycontainer.append([pageUrl]);
    var details = $(`<div>URL: <a href="${url}">${url}</a><br>Last Modified: ${lastModifiedDate}, Size of Page: ${sizeofPage}kb</div>`);
    detailsContainer.append(details);

    //keywords
    var keywordArray = response.results[i].wordFrequencies; // Get the array of keywords
    var keywordText ="";
    // Check if keyword array exists
    if (keywordArray && keywordArray.length > 0) {
      // Loop through each keyword and concatenate it to the keywordText string
      for (var j = 0; j < keywordArray.length; j++) {
          keywordText += `${keywordArray[j].word}: ${keywordArray[j].frequency}`;
          // Add a comma if it's not the last keyword
          if (j !== keywordArray.length - 1) {
              keywordText += "; ";
          }
      }
      var keywordsDiv = $('<div class="keywords">Keywords: ' + keywordText + '</div>');
      detailsContainer.append(keywordsDiv);
    } else {
        detailsContainer.append('<div class="keywords">No keywords available</div>');
    }

    //child links
    var childLinks = response.results[i].childLinks; // Get the array of child links
    var childLinksText = $('<div class="child-links">Child Links:</div>');
    // Check if child links exist
    if (childLinks && childLinks.length > 0) {
        for (var j = 0; j < childLinks.length; j++) {
            var childLinkDiv = $(`<div>${childLinks[j]}</div>`);
            childLinksText.append(childLinkDiv);
        }
    } else {
        // If no child links exist, display a message
        childLinksText.append('<div>No child links available</div>');
    }

    // Append the child links container to the details container
    detailsContainer.append(childLinksText);


    var parentLinksContainer = $('<div></div>'); // Container for parent links
    var parentLinks = response.results[i].parentLinks; // Get the array of parent links

    // Parent links
    var parentLinks = response.results[i].parentLinks;
    var parentLinksText = $('<div class="parent-links">Parent Links:</div>');
    if (parentLinks && parentLinks.length > 0) {
        for (var k = 0; k < parentLinks.length; k++) {
            var parentLinkDiv = $(`<div>${parentLinks[k]}</div>`);
            parentLinksText.append(parentLinkDiv);
        }
    } else {
        parentLinksText.append('<div>No parent links available</div>');
    }
    detailsContainer.append(parentLinksText);

    container.append(detailsContainer);
    
    var showMoreButton = $('<button class="show-more-btn">Show More</button>');
    container.append(showMoreButton);

    // Add event listener to toggle visibility of details
    showMoreButton.on('click', function() {
        var details = $(this).siblings('.details');
        details.toggle();

        //Change button text based on visibility
        var buttonText = details.is(':visible') ? 'Hide' : 'Show More';
        $(this).text(buttonText);
    });


    $("#searchResults").append(container);
  }
}

