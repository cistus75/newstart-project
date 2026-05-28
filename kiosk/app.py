from flask import Flask, render_template, send_from_directory

app = Flask(__name__)

@app.route('/')
def index():
    return render_template('index.html')

# /images/파일명 요청을 static/images/ 에서 찾아 반환
@app.route('/images/<path:filename>')
def images(filename):
    return send_from_directory('static/images', filename)

if __name__ == '__main__':
    # host='0.0.0.0' : 외부에서 접근 가능
    # port=5000 : 원하는 포트로 변경 가능
    app.run(host='0.0.0.0', port=5000, debug=False)
