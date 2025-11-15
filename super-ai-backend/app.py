from flask import Flask, request, jsonify

app = Flask(__name__)

@app.route("/", methods=["GET"])
def home():
    return jsonify({"message": "Super AI Backend Running!"})

@app.route("/api/generate", methods=["POST"])
def generate():
    try:
        data = request.json
        user_message = data.get("message", "")

        # Dummy response for now
        reply = f"AI Response: You said → {user_message}"

        return jsonify({
            "success": True,
            "reply": reply
        })
    except Exception as e:
        return jsonify({
            "success": False,
            "error": str(e)
        }), 500

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=10000)
