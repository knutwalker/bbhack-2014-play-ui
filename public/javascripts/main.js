$(function () {
  var container = $("#dump");

  var maximum = container.outerWidth() / 2 || 300;

  var series = Array.apply(null, {length: 10}).map(function() {
    return {
      data: [],
      values: [],
      label: "",
      lines: {
        fill: true
      }
    };
  });

  var options = {
    margin: 0,
    series: {
      shadowSize: 0,
      lines: { show: true, fill: 0.05, steps: false, lineColor: "#DBDBD9", lineWidth: 2 },
      points: { show: false, radius: 0 }
    },
    legend: {
      position: "nw",
      backgroundOpacity: 0.5
    },
    grid: {
      show: true,
      aboveData: true,
      hoverable: true,
      margin: 0,
      color: "#9B9B99",
      backgroundColor: "#e9e9e9",
      borderWidth: 1
    },
    yaxis: {
      axisLabel: "Value",
      position: 'right'
    },
    xaxis: {
      show: false
    }
  };

  var plot = $.plot(container, series, options);

  function generateData(values, data) {
    if (values.length > maximum) {
      values.splice(0, 1);
    }
    values.push(data);

    return values.map(function(y, x) {
      return [x, y];
    });
  }


  var ws = new WebSocket("ws://" + location.host + "/ws");
  ws.onmessage = _.throttle(function (e) {

    JSON.parse(e.data).forEach(function(data, idx) {
      series[idx].data = generateData(series[idx].values, data.sketchCount);
      series[idx].label = data.tag;
    });
    plot.setData(series);
    plot.setupGrid();
    plot.draw();
  }, 50);
});
