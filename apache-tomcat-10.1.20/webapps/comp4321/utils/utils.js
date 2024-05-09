$(document).ready(function () {
  $("#searchForm").submit(function (event) {
    // Prevent the default form submission behavior
    event.preventDefault();
    var input = $("#searchInput").val();
    // Call your function or perform any other actions here
    getPages(input); // Call your function to fetch data
  });
});

function getPages(input) {
  $.ajax({
    url: "../comp4321/apis/getData.jsp", // Endpoint URL to fetch data
    data: { input: input }, // Data to be sent to the server
    type: "GET", // HTTP method
    contentType: "application/json",
    //dataType: "text", // Data type expected from the server
    success: function (data) {
      // Handle successful response
      // Process the data and update the UI as needed
      // console.log(data);
      showResults(data); //in utils.js
    },
    error: function (xhr, status, error) {
      // Handle error response
      console.error(xhr.responseText); // Log the error message
    },
  });
};
